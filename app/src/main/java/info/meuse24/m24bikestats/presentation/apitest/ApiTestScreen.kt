package info.meuse24.m24bikestats.presentation.apitest

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.meuse24.m24bikestats.R
import info.meuse24.m24bikestats.api.BoschEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Stateless API-Test-Screen. Kein ViewModel-Zugriff – nur [ApiTestUiState] + Callbacks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiTestScreen(
    uiState: ApiTestUiState,
    onSelectEndpoint: (BoschEndpoint) -> Unit,
    onFetch: () -> Unit,
    onRunAll: () -> Unit,
    onClear: () -> Unit,
    onLogout: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.api_test_title)) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = stringResource(R.string.nav_logout))
                    }
                }
            )
        }
    ) { padding ->
        ApiTestContent(
            uiState = uiState,
            onSelectEndpoint = onSelectEndpoint,
            onFetch = onFetch,
            onRunAll = onRunAll,
            onClear = onClear,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
fun ApiTestContent(
    uiState: ApiTestUiState,
    onSelectEndpoint: (BoschEndpoint) -> Unit,
    onFetch: () -> Unit,
    onRunAll: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val readableResult = remember(uiState.selectedEndpoint, uiState.jsonOutput) {
        parseReadableResult(uiState.selectedEndpoint, uiState.jsonOutput)
    }
    val responseDiagnostics = remember(uiState.selectedEndpoint, uiState.jsonOutput) {
        buildApiTestResponseDiagnostics(uiState.selectedEndpoint, uiState.jsonOutput)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        EndpointDropdown(
            selected = uiState.selectedEndpoint,
            onSelect = onSelectEndpoint,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${uiState.selectedEndpoint.baseUrl}${uiState.selectedEndpoint.path}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
        )

        Spacer(modifier = Modifier.height(12.dp))

        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = onFetch, enabled = !uiState.isLoading) {
                Text(stringResource(R.string.api_test_fetch))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onRunAll, enabled = !uiState.isLoading) {
                Text(stringResource(R.string.api_test_run_all))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        val result = withContext(Dispatchers.IO) {
                            saveApiTestResultToDownloads(
                                context = context,
                                endpoint = uiState.selectedEndpoint,
                                content = uiState.jsonOutput,
                            )
                        }
                        val message = result.fold(
                            onSuccess = { fileName ->
                                context.getString(R.string.api_test_download_saved, fileName)
                            },
                            onFailure = { error ->
                                context.getString(
                                    R.string.api_test_download_failed,
                                    error.message ?: error.javaClass.simpleName,
                                )
                            }
                        )
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = uiState.jsonOutput.isNotEmpty() && !uiState.isLoading,
            ) {
                Text(stringResource(R.string.api_test_download))
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val intent = createApiTestShareIntent(
                        context = context,
                        endpoint = uiState.selectedEndpoint,
                        content = uiState.jsonOutput,
                    )
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.api_test_share_json)))
                },
                enabled = uiState.jsonOutput.isNotEmpty(),
            ) {
                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.api_test_share))
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onClear, enabled = uiState.jsonOutput.isNotEmpty()) {
                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.api_test_clear_output))
            }
            if (uiState.isLoading) {
                Spacer(modifier = Modifier.width(12.dp))
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (readableResult != null) {
            ReadableResultSection(readableResult)
            Spacer(modifier = Modifier.height(12.dp))
        }

        responseDiagnostics?.unusedFieldLines
            ?.takeIf { it.isNotEmpty() }
            ?.let { lines ->
                SimpleInfoCard(
                    title = stringResource(R.string.api_test_field_scan_title),
                    lines = lines,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

        Text(stringResource(R.string.api_test_raw_data), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        JsonOutputBox(json = uiState.jsonOutput)
    }
}

@Composable
private fun EndpointDropdown(
    selected: BoschEndpoint,
    onSelect: (BoschEndpoint) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.api_test_endpoint)) },
            modifier = Modifier
                .fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .matchParentSize(),
        ) {
            Button(
                onClick = { expanded = true },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
            ) {
                Text(stringResource(R.string.common_select))
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            BoschEndpoint.entries.forEach { endpoint ->
                DropdownMenuItem(
                    text = { Text(endpoint.label) },
                    onClick = {
                        onSelect(endpoint)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ReadableResultSection(result: BoschReadableResult) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.api_test_readable_view), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        when (result) {
            is BoschReadableResult.Activities -> ActivitiesSection(result)
            is BoschReadableResult.BikeList -> BikeListSection(result)
            is BoschReadableResult.BikeDetail -> BikeDetailSection(result)
            is BoschReadableResult.BikePass -> SimpleInfoCard(
                title = stringResource(R.string.api_test_bike_pass_title),
                lines = listOfNotNull(
                    result.frameNumber?.let { stringResource(R.string.api_test_bike_pass_frame_number, it) },
                    result.frameNumberPosition?.let { stringResource(R.string.api_test_bike_pass_frame_position, it) },
                    result.description?.let { stringResource(R.string.api_test_bike_pass_description, it) },
                    stringResource(R.string.api_test_bike_pass_theft_reports, result.theftReportCount.toString()),
                )
            )
            is BoschReadableResult.ServiceBook -> SimpleInfoCard(
                title = stringResource(R.string.api_test_service_book_title),
                lines = listOfNotNull(
                    stringResource(R.string.api_test_service_book_count, result.recordCount.toString()),
                    result.latestType?.let { stringResource(R.string.api_test_service_book_latest_type, it) },
                    result.latestDealer?.let { stringResource(R.string.api_test_service_book_latest_dealer, it) },
                )
            )
            is BoschReadableResult.Registrations -> SimpleInfoCard(
                title = stringResource(R.string.api_test_registrations_title),
                lines = listOf(
                    stringResource(R.string.api_test_registrations_count, result.registrationCount.toString()),
                    stringResource(R.string.api_test_registrations_bikes, result.bikeRegistrationCount.toString()),
                    stringResource(R.string.api_test_registrations_components, result.componentRegistrationCount.toString()),
                )
            )
            is BoschReadableResult.UserInfo -> SimpleInfoCard(
                title = stringResource(R.string.api_test_user_profile_title),
                lines = listOf(
                    stringResource(R.string.api_test_user_profile_email, result.email),
                    stringResource(R.string.api_test_user_profile_username, result.username),
                    stringResource(R.string.api_test_user_profile_subject, result.subject),
                )
            )
            is BoschReadableResult.TokenInfo -> SimpleInfoCard(
                title = stringResource(R.string.api_test_token_overview_title),
                lines = listOfNotNull(
                    stringResource(R.string.api_test_token_audience, result.audience.joinToString()),
                    stringResource(R.string.api_test_token_scope, result.scope),
                    result.expiresAt?.let { stringResource(R.string.api_test_token_expires, it) },
                    result.boschId?.let { stringResource(R.string.api_test_token_bosch_id, it) },
                    result.riderId?.let { stringResource(R.string.api_test_token_rider_id, it) },
                )
            )
            is BoschReadableResult.OidcDiscovery -> SimpleInfoCard(
                title = stringResource(R.string.api_test_oidc_title),
                lines = listOf(
                    stringResource(R.string.api_test_oidc_issuer, result.issuer),
                    stringResource(R.string.api_test_oidc_authorization, result.authorizationEndpoint),
                    stringResource(R.string.api_test_oidc_token, result.tokenEndpoint),
                    stringResource(R.string.api_test_oidc_userinfo, result.userInfoEndpoint),
                    stringResource(R.string.api_test_oidc_grant_types, result.supportedGrants.joinToString()),
                )
            )
        }
    }
}

@Composable
private fun ActivitiesSection(result: BoschReadableResult.Activities) {
    SimpleInfoCard(
        title = stringResource(R.string.api_test_activities_title, result.total),
        lines = listOf(stringResource(R.string.api_test_activities_shown, result.items.size))
    )
    Spacer(modifier = Modifier.height(8.dp))
    result.items.take(5).forEach { activity ->
        InfoCard(
            title = activity.title,
            subtitle = activity.startedAt,
            lines = listOfNotNull(
                stringResource(R.string.api_test_activity_distance, activity.distanceKm),
                stringResource(R.string.api_test_activity_duration, activity.duration),
                stringResource(R.string.api_test_activity_speed, activity.averageSpeed, activity.maxSpeed),
                activity.cadence?.let { stringResource(R.string.api_test_activity_cadence, it) },
                activity.riderPower?.let { stringResource(R.string.api_test_activity_power, it) },
                activity.elevation?.let { stringResource(R.string.api_test_activity_elevation, it) },
                activity.calories?.let { stringResource(R.string.api_test_activity_calories, it) },
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun BikeListSection(result: BoschReadableResult.BikeList) {
    Text(stringResource(R.string.api_test_bikes_count, result.bikes.size), style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    result.bikes.forEach { bike ->
        BikeCard(bike)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun BikeDetailSection(result: BoschReadableResult.BikeDetail) {
    BikeCard(result.bike)
}

@Composable
private fun BikeCard(bike: BikeItem) {
    InfoCard(
        title = bike.driveUnitName,
        subtitle = bike.headUnitName,
        lines = buildList {
            add(stringResource(R.string.api_test_bike_id, bike.id))
            bike.remoteName?.let { add(stringResource(R.string.api_test_bike_remote, it)) }
            bike.odometerKm?.let { add(stringResource(R.string.api_test_bike_odometer, it)) }
            bike.assistSpeedKmh?.let { add(stringResource(R.string.api_test_bike_assist_speed, it)) }
            if (bike.batterySummary.isNotEmpty()) {
                add(stringResource(R.string.api_test_bike_batteries, bike.batterySummary.joinToString(" | ")))
            }
            if (bike.assistModes.isNotEmpty()) {
                add(stringResource(R.string.api_test_bike_assist_modes, bike.assistModes.joinToString()))
            }
        }
    )
}

@Composable
private fun SimpleInfoCard(
    title: String,
    lines: List<String>,
) {
    InfoCard(title = title, subtitle = null, lines = lines)
}

@Composable
private fun InfoCard(
    title: String,
    subtitle: String?,
    lines: List<String>,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            lines.forEach { line ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun JsonOutputBox(
    json: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp, max = 420.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Text(
                text = json.ifEmpty { stringResource(R.string.api_test_no_result) },
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = if (json.isEmpty())
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
