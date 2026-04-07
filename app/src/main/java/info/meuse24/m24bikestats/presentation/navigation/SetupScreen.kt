package info.meuse24.m24bikestats.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import info.meuse24.m24bikestats.R
import info.meuse24.m24bikestats.domain.model.BackgroundSyncMode
import info.meuse24.m24bikestats.domain.model.CloudSyncDetailMode
import info.meuse24.m24bikestats.domain.model.CsvExportFormat

@Composable
fun SetupScreen(
    csvExportFormat: CsvExportFormat,
    cloudSyncDetailMode: CloudSyncDetailMode,
    backgroundSyncMode: BackgroundSyncMode,
    onCsvExportFormatSelected: (CsvExportFormat) -> Unit,
    onCloudSyncDetailModeSelected: (CloudSyncDetailMode) -> Unit,
    onBackgroundSyncModeSelected: (BackgroundSyncMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            SetupSectionTitle(text = stringResource(R.string.setup_csv_title))
        }
        items(CsvExportFormat.entries) { format ->
            SetupOptionCard(
                selected = format == csvExportFormat,
                label = stringResource(format.labelRes()),
                onClick = { onCsvExportFormatSelected(format) },
            )
        }
        item {
            SetupSectionTitle(text = stringResource(R.string.setup_sync_title))
        }
        items(CloudSyncDetailMode.entries) { mode ->
            SetupOptionCard(
                selected = mode == cloudSyncDetailMode,
                label = stringResource(mode.labelRes()),
                onClick = { onCloudSyncDetailModeSelected(mode) },
            )
        }
        item {
            SetupSectionTitle(text = stringResource(R.string.setup_background_sync_title))
        }
        items(BackgroundSyncMode.entries) { mode ->
            SetupOptionCard(
                selected = mode == backgroundSyncMode,
                label = stringResource(mode.labelRes()),
                onClick = { onBackgroundSyncModeSelected(mode) },
            )
        }
    }
}

@Composable
private fun SetupSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
    )
}

@Composable
private fun SetupOptionCard(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                modifier = Modifier.fillMaxWidth(0.82f),
            )
            RadioButton(
                selected = selected,
                onClick = onClick,
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
