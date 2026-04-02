package info.meuse24.m24bikestats.presentation.apitest

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
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.meuse24.m24bikestats.domain.model.BoschEndpoint

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
                title = { Text("API Test") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Abmelden")
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onFetch, enabled = !uiState.isLoading) {
                Text("Abrufen")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onRunAll, enabled = !uiState.isLoading) {
                Text("Alle testen")
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, uiState.selectedEndpoint.label)
                        putExtra(Intent.EXTRA_TEXT, uiState.jsonOutput)
                    }
                    context.startActivity(Intent.createChooser(intent, "JSON teilen"))
                },
                enabled = uiState.jsonOutput.isNotEmpty(),
            ) {
                Icon(Icons.Default.Share, contentDescription = "Teilen")
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onClear, enabled = uiState.jsonOutput.isNotEmpty()) {
                Icon(Icons.Default.Clear, contentDescription = "Ausgabe leeren")
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

        Text("Rohdaten", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        JsonOutputBox(json = uiState.jsonOutput)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EndpointDropdown(
    selected: BoschEndpoint,
    onSelect: (BoschEndpoint) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Endpunkt") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
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
        Text("Verständliche Ansicht", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        when (result) {
            is BoschReadableResult.Activities -> ActivitiesSection(result)
            is BoschReadableResult.BikeList -> BikeListSection(result)
            is BoschReadableResult.BikeDetail -> BikeDetailSection(result)
            is BoschReadableResult.UserInfo -> SimpleInfoCard(
                title = "Bosch Nutzerprofil",
                lines = listOf(
                    "E-Mail: ${result.email}",
                    "Benutzer: ${result.username}",
                    "Subject: ${result.subject}",
                )
            )
            is BoschReadableResult.TokenInfo -> SimpleInfoCard(
                title = "Token-Überblick",
                lines = listOfNotNull(
                    "Audience: ${result.audience.joinToString()}",
                    "Scope: ${result.scope}",
                    result.expiresAt?.let { "Läuft ab: $it" },
                    result.boschId?.let { "Bosch-ID: $it" },
                    result.riderId?.let { "Rider-ID: $it" },
                )
            )
            is BoschReadableResult.OidcDiscovery -> SimpleInfoCard(
                title = "OIDC Discovery",
                lines = listOf(
                    "Issuer: ${result.issuer}",
                    "Authorization: ${result.authorizationEndpoint}",
                    "Token: ${result.tokenEndpoint}",
                    "UserInfo: ${result.userInfoEndpoint}",
                    "Grant Types: ${result.supportedGrants.joinToString()}",
                )
            )
        }
    }
}

@Composable
private fun ActivitiesSection(result: BoschReadableResult.Activities) {
    SimpleInfoCard(
        title = "Aktivitäten (${result.total})",
        lines = listOf("Angezeigt: ${result.items.size}")
    )
    Spacer(modifier = Modifier.height(8.dp))
    result.items.take(5).forEach { activity ->
        InfoCard(
            title = activity.title,
            subtitle = activity.startedAt,
            lines = listOfNotNull(
                "Distanz: ${activity.distanceKm}",
                "Dauer: ${activity.duration}",
                "Geschwindigkeit: ${activity.averageSpeed} Ø, ${activity.maxSpeed} max",
                activity.cadence?.let { "Kadenz: $it" },
                activity.riderPower?.let { "Leistung: $it" },
                activity.elevation?.let { "Höhenmeter: $it" },
                activity.calories?.let { "Kalorien: $it" },
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun BikeListSection(result: BoschReadableResult.BikeList) {
    Text("Bikes (${result.bikes.size})", style = MaterialTheme.typography.titleMedium)
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
            add("Bike-ID: ${bike.id}")
            bike.remoteName?.let { add("Remote: $it") }
            bike.odometerKm?.let { add("Kilometerstand: $it") }
            bike.assistSpeedKmh?.let { add("Max. Unterstützungs-Geschwindigkeit: $it") }
            if (bike.batterySummary.isNotEmpty()) {
                add("Batterien: ${bike.batterySummary.joinToString(" | ")}")
            }
            if (bike.assistModes.isNotEmpty()) {
                add("Assist Modes: ${bike.assistModes.joinToString()}")
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
                text = json.ifEmpty { "– noch kein Ergebnis –" },
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
