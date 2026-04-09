package info.meuse24.m24bikestats.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import info.meuse24.m24bikestats.R
import info.meuse24.m24bikestats.domain.model.BackgroundSyncMode
import info.meuse24.m24bikestats.domain.model.CloudSyncDetailMode
import info.meuse24.m24bikestats.domain.model.CsvExportFormat
import info.meuse24.m24bikestats.domain.model.DisplayMode

@Composable
fun SetupScreen(
    displayMode: DisplayMode,
    csvExportFormat: CsvExportFormat,
    cloudSyncDetailMode: CloudSyncDetailMode,
    backgroundSyncMode: BackgroundSyncMode,
    showExplanationTexts: Boolean,
    onDisplayModeSelected: (DisplayMode) -> Unit,
    onCsvExportFormatSelected: (CsvExportFormat) -> Unit,
    onCloudSyncDetailModeSelected: (CloudSyncDetailMode) -> Unit,
    onBackgroundSyncModeSelected: (BackgroundSyncMode) -> Unit,
    onShowExplanationTextsChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayModeOptions = DisplayMode.entries.map { it to stringResource(it.labelRes()) }
    val csvOptions = CsvExportFormat.entries.map { it to stringResource(it.labelRes()) }
    val cloudSyncOptions = CloudSyncDetailMode.entries.map { it to stringResource(it.labelRes()) }
    val backgroundSyncOptions = BackgroundSyncMode.entries.map { it to stringResource(it.labelRes()) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (showExplanationTexts) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.setup_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.92f),
                        modifier = Modifier.padding(18.dp),
                    )
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    SetupTogglePreference(
                        title = stringResource(R.string.setup_explanation_texts_title),
                        subtitle = stringResource(R.string.setup_explanation_texts_subtitle).takeIf {
                            showExplanationTexts
                        },
                        checked = showExplanationTexts,
                        onCheckedChange = onShowExplanationTextsChanged,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    SetupDropdownPreference(
                        title = stringResource(R.string.setup_display_mode_title),
                        subtitle = stringResource(R.string.setup_display_mode_subtitle).takeIf {
                            showExplanationTexts
                        },
                        selectedLabel = stringResource(displayMode.labelRes()),
                        options = displayModeOptions,
                        onOptionSelected = onDisplayModeSelected,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    SetupDropdownPreference(
                        title = stringResource(R.string.setup_csv_title),
                        subtitle = stringResource(R.string.setup_csv_subtitle).takeIf {
                            showExplanationTexts
                        },
                        selectedLabel = stringResource(csvExportFormat.labelRes()),
                        options = csvOptions,
                        onOptionSelected = onCsvExportFormatSelected,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    SetupDropdownPreference(
                        title = stringResource(R.string.setup_sync_title),
                        subtitle = stringResource(R.string.setup_sync_subtitle).takeIf {
                            showExplanationTexts
                        },
                        selectedLabel = stringResource(cloudSyncDetailMode.labelRes()),
                        options = cloudSyncOptions,
                        onOptionSelected = onCloudSyncDetailModeSelected,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    SetupDropdownPreference(
                        title = stringResource(R.string.setup_background_sync_title),
                        subtitle = stringResource(R.string.setup_background_sync_subtitle).takeIf {
                            showExplanationTexts
                        },
                        selectedLabel = stringResource(backgroundSyncMode.labelRes()),
                        options = backgroundSyncOptions,
                        onOptionSelected = onBackgroundSyncModeSelected,
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> SetupDropdownPreference(
    title: String,
    subtitle: String?,
    selectedLabel: String,
    options: List<Pair<T, String>>,
    onOptionSelected: (T) -> Unit,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text(
                    text = selectedLabel,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            expanded = false
                            onOptionSelected(value)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupTogglePreference(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

private fun CsvExportFormat.labelRes(): Int = when (this) {
    CsvExportFormat.SYSTEM_DEFAULT -> R.string.csv_export_format_system_default_label
    CsvExportFormat.EXCEL_DE -> R.string.csv_export_format_excel_de_label
    CsvExportFormat.STANDARD_INTERNATIONAL -> R.string.csv_export_format_standard_international_label
}

private fun CloudSyncDetailMode.labelRes(): Int = when (this) {
    CloudSyncDetailMode.MISSING_ONLY -> R.string.cloud_sync_detail_mode_missing_only_label
    CloudSyncDetailMode.MISSING_OR_STALE -> R.string.cloud_sync_detail_mode_missing_or_stale_label
}

private fun BackgroundSyncMode.labelRes(): Int = when (this) {
    BackgroundSyncMode.DISABLED -> R.string.background_sync_mode_disabled_label
    BackgroundSyncMode.DAILY_UNMETERED -> R.string.background_sync_mode_daily_unmetered_label
    BackgroundSyncMode.DAILY_CONNECTED -> R.string.background_sync_mode_daily_connected_label
}

private fun DisplayMode.labelRes(): Int = when (this) {
    DisplayMode.AUTOMATIC -> R.string.display_mode_automatic_label
    DisplayMode.LIGHT -> R.string.display_mode_light_label
    DisplayMode.DARK -> R.string.display_mode_dark_label
}
