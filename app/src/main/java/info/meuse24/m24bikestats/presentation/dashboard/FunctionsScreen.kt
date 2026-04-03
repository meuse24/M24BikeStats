package info.meuse24.m24bikestats.presentation.dashboard

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import info.meuse24.m24bikestats.R
import info.meuse24.m24bikestats.domain.model.CsvExportFormat

@Composable
fun FunctionsScreen(
    uiState: FunctionsUiState,
    onExportActivitiesCsv: () -> Unit,
    onExportActivityDetailsCsv: () -> Unit,
    onActivitiesCsvExportHandled: () -> Unit,
    onActivityDetailsCsvExportHandled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    LaunchedEffect(uiState.pendingActivitiesCsvExport) {
        val export = uiState.pendingActivitiesCsvExport ?: return@LaunchedEffect
        val csvUri = createActivitiesCsvUri(context, export)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, csvUri)
            putExtra(Intent.EXTRA_SUBJECT, export.fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.functions_share_chooser_csv)))
        onActivitiesCsvExportHandled()
    }

    LaunchedEffect(uiState.pendingActivityDetailsCsvExport) {
        val export = uiState.pendingActivityDetailsCsvExport ?: return@LaunchedEffect
        val csvUri = createActivityDetailsCsvUri(context, export)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, csvUri)
            putExtra(Intent.EXTRA_SUBJECT, export.fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.functions_share_chooser_detail_csv)))
        onActivityDetailsCsvExportHandled()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            HeroCard(
                eyebrow = stringResource(R.string.functions_hero_eyebrow),
                title = stringResource(R.string.functions_hero_title),
                subtitle = stringResource(R.string.functions_hero_subtitle),
            ) {
                SummaryChipRow(
                    listOf(
                        stringResource(R.string.activities_metric_loaded) to uiState.loadedActivityCount.toString(),
                        stringResource(R.string.activities_metric_total) to uiState.activityTotalCount.toString(),
                        stringResource(R.string.activities_metric_visible) to uiState.visibleActivityCount.toString(),
                    )
                )
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.functions_export_activities_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.functions_export_activities_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SectionSurface {
                        OptionalRow(stringResource(R.string.functions_label_csv_format), stringResource(uiState.csvExportFormat.labelRes()))
                    }
                    uiState.lastActivitiesCsvExport?.let { exportSummary ->
                        SectionSurface {
                            Text(
                                text = stringResource(R.string.functions_last_exported),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            OptionalRow(stringResource(R.string.functions_label_file), exportSummary.fileName)
                            OptionalRow(stringResource(R.string.functions_label_activities), exportSummary.activityCount.toString())
                            OptionalRow(stringResource(R.string.functions_label_exported_at), exportSummary.exportedAtLabel)
                        }
                    }
                    if (uiState.isExportingActivitiesCsv) {
                        val totalCount = uiState.exportTotalActivityCount
                        val loadedCount = uiState.exportLoadedActivityCount
                        val progress = if (totalCount > 0) {
                            loadedCount.toFloat() / totalCount.toFloat()
                        } else {
                            0f
                        }

                        Text(
                            text = stringResource(R.string.functions_progress_loaded, loadedCount, totalCount),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Button(
                        onClick = onExportActivitiesCsv,
                        enabled = !uiState.isExportingActivitiesCsv &&
                            !uiState.isExportingActivityDetailsCsv &&
                            !uiState.isInitialLoading &&
                            !uiState.isRefreshing,
                    ) {
                        if (uiState.isExportingActivitiesCsv) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height(18.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(stringResource(R.string.functions_export_running))
                        } else {
                            Text(stringResource(R.string.functions_export_button))
                        }
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.functions_export_details_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.functions_export_details_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SectionSurface {
                        OptionalRow(stringResource(R.string.functions_label_csv_format), stringResource(uiState.csvExportFormat.labelRes()))
                        OptionalRow(stringResource(R.string.functions_label_visible_activities), uiState.visibleActivityCount.toString())
                        OptionalRow(stringResource(R.string.functions_label_loaded_activities), uiState.loadedActivityCount.toString())
                    }
                    uiState.lastActivityDetailsCsvExport?.let { exportSummary ->
                        SectionSurface {
                            Text(
                                text = stringResource(R.string.functions_last_exported),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            OptionalRow(stringResource(R.string.functions_label_file), exportSummary.fileName)
                            OptionalRow(stringResource(R.string.functions_label_activities), exportSummary.activityCount.toString())
                            OptionalRow(stringResource(R.string.functions_label_detail_points), exportSummary.detailPointCount.toString())
                            OptionalRow(stringResource(R.string.functions_label_exported_at), exportSummary.exportedAtLabel)
                        }
                    }
                    if (uiState.isExportingActivityDetailsCsv) {
                        val totalCount = uiState.exportDetailedTotalActivityCount
                        val loadedCount = uiState.exportDetailedLoadedActivityCount
                        val progress = if (totalCount > 0) {
                            loadedCount.toFloat() / totalCount.toFloat()
                        } else {
                            0f
                        }

                        Text(
                            text = stringResource(R.string.functions_progress_loaded, loadedCount, totalCount),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Button(
                        onClick = onExportActivityDetailsCsv,
                        enabled = uiState.visibleActivityCount > 0 &&
                            !uiState.isExportingActivitiesCsv &&
                            !uiState.isExportingActivityDetailsCsv &&
                            !uiState.isInitialLoading &&
                            !uiState.isRefreshing,
                    ) {
                        if (uiState.isExportingActivityDetailsCsv) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height(18.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(stringResource(R.string.functions_detail_export_running))
                        } else {
                            Text(stringResource(R.string.functions_detail_export_button))
                        }
                    }
                }
            }
        }
    }
}

private fun CsvExportFormat.labelRes(): Int = when (this) {
    CsvExportFormat.SYSTEM_DEFAULT -> R.string.csv_export_format_system_default_label
    CsvExportFormat.EXCEL_DE -> R.string.csv_export_format_excel_de_label
    CsvExportFormat.STANDARD_INTERNATIONAL -> R.string.csv_export_format_standard_international_label
}
