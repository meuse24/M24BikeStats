package info.meuse24.m24bikestats.presentation.dashboard

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
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
    onExportPdf: () -> Unit,
    onCancelActivitiesCsvExport: () -> Unit,
    onCancelActivityDetailsCsvExport: () -> Unit,
    onCancelPdfExport: () -> Unit,
    onActivitiesCsvExportHandled: () -> Unit,
    onActivityDetailsCsvExportHandled: () -> Unit,
    onPdfExportHandled: () -> Unit,
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

    LaunchedEffect(uiState.pendingPdfExport) {
        val export = uiState.pendingPdfExport ?: return@LaunchedEffect
        val pdfUri = createPdfReportUri(context, export)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            putExtra(Intent.EXTRA_SUBJECT, export.fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.functions_share_chooser_pdf)))
        onPdfExportHandled()
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
            FunctionsExportCard(
                title = stringResource(R.string.functions_export_pdf_title),
                subtitle = stringResource(R.string.functions_export_pdf_subtitle),
                summary = listOf(
                    stringResource(R.string.functions_chip_format) to stringResource(R.string.functions_chip_pdf),
                    stringResource(R.string.functions_chip_scope) to stringResource(R.string.functions_scope_full_summary),
                    stringResource(R.string.functions_chip_rows) to stringResource(R.string.functions_rows_pdf_sections),
                ),
                isRunning = uiState.isExportingPdf,
                progressCurrent = 0,
                progressTotal = 0,
                onClick = onExportPdf,
                onCancel = onCancelPdfExport,
                enabled = !uiState.isExportingActivitiesCsv &&
                    !uiState.isExportingActivityDetailsCsv &&
                    !uiState.isExportingPdf &&
                    !uiState.isInitialLoading &&
                    !uiState.isRefreshing,
                idleButtonLabel = stringResource(R.string.functions_export_pdf_button),
                runningButtonLabel = stringResource(R.string.functions_export_pdf_running),
            ) {
                uiState.lastPdfExport?.let { exportSummary ->
                    ExportSummarySection(
                        rows = listOf(
                            stringResource(R.string.functions_label_file) to exportSummary.fileName,
                            stringResource(R.string.functions_label_exported_at) to exportSummary.exportedAtLabel,
                        )
                    )
                }
            }
        }
        item {
            FunctionsExportCard(
                title = stringResource(R.string.functions_export_activities_title),
                subtitle = stringResource(R.string.functions_export_activities_subtitle),
                summary = listOf(
                    stringResource(R.string.functions_chip_csv) to stringResource(uiState.csvExportFormat.labelRes()),
                    stringResource(R.string.functions_chip_scope) to stringResource(R.string.functions_scope_all_activities),
                    stringResource(R.string.functions_chip_rows) to stringResource(R.string.functions_rows_one_per_activity),
                ),
                isRunning = uiState.isExportingActivitiesCsv,
                progressCurrent = uiState.exportLoadedActivityCount,
                progressTotal = uiState.exportTotalActivityCount,
                onClick = onExportActivitiesCsv,
                onCancel = onCancelActivitiesCsvExport,
                enabled = !uiState.isExportingActivitiesCsv &&
                    !uiState.isExportingActivityDetailsCsv &&
                    !uiState.isExportingPdf &&
                    !uiState.isInitialLoading &&
                    !uiState.isRefreshing,
                idleButtonLabel = stringResource(R.string.functions_export_button),
                runningButtonLabel = stringResource(R.string.functions_export_running),
            ) {
                uiState.lastActivitiesCsvExport?.let { exportSummary ->
                    ExportSummarySection(
                        rows = listOf(
                            stringResource(R.string.functions_label_file) to exportSummary.fileName,
                            stringResource(R.string.functions_label_activities) to exportSummary.activityCount.toString(),
                            stringResource(R.string.functions_label_exported_at) to exportSummary.exportedAtLabel,
                        )
                    )
                }
            }
        }
        item {
            FunctionsExportCard(
                title = stringResource(R.string.functions_export_details_title),
                subtitle = stringResource(R.string.functions_export_details_subtitle),
                summary = listOf(
                    stringResource(R.string.functions_chip_csv) to stringResource(uiState.csvExportFormat.labelRes()),
                    stringResource(R.string.functions_chip_scope) to stringResource(R.string.functions_scope_visible_activities),
                    stringResource(R.string.functions_chip_rows) to stringResource(R.string.functions_rows_detail_points),
                ),
                isRunning = uiState.isExportingActivityDetailsCsv,
                progressCurrent = uiState.exportDetailedLoadedActivityCount,
                progressTotal = uiState.exportDetailedTotalActivityCount,
                onClick = onExportActivityDetailsCsv,
                onCancel = onCancelActivityDetailsCsvExport,
                enabled = uiState.visibleActivityCount > 0 &&
                    !uiState.isExportingActivitiesCsv &&
                    !uiState.isExportingActivityDetailsCsv &&
                    !uiState.isExportingPdf &&
                    !uiState.isInitialLoading &&
                    !uiState.isRefreshing,
                idleButtonLabel = stringResource(R.string.functions_detail_export_button),
                runningButtonLabel = stringResource(R.string.functions_detail_export_running),
            ) {
                SectionSurface {
                    OptionalRow(
                        stringResource(R.string.functions_label_visible_activities),
                        uiState.visibleActivityCount.toString(),
                    )
                    OptionalRow(
                        stringResource(R.string.functions_label_loaded_activities),
                        uiState.loadedActivityCount.toString(),
                    )
                }
                uiState.lastActivityDetailsCsvExport?.let { exportSummary ->
                    ExportSummarySection(
                        rows = listOf(
                            stringResource(R.string.functions_label_file) to exportSummary.fileName,
                            stringResource(R.string.functions_label_activities) to exportSummary.activityCount.toString(),
                            stringResource(R.string.functions_label_detail_points) to exportSummary.detailPointCount.toString(),
                            stringResource(R.string.functions_label_exported_at) to exportSummary.exportedAtLabel,
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun FunctionsExportCard(
    title: String,
    subtitle: String,
    summary: List<Pair<String, String>>,
    isRunning: Boolean,
    progressCurrent: Int,
    progressTotal: Int,
    onClick: () -> Unit,
    onCancel: () -> Unit,
    enabled: Boolean,
    idleButtonLabel: String,
    runningButtonLabel: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SummaryChipRow(
                    summary = summary,
                    itemContent = { label, value ->
                        CompactMetricPill(label = label, value = value)
                    },
                )
                content()
                if (isRunning) {
                    if (progressCurrent > 0 || progressTotal > 0) {
                        ExportProgressSection(
                            current = progressCurrent,
                            total = progressTotal,
                        )
                    }
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.functions_cancel_button))
                    }
                }
                Button(
                    onClick = onClick,
                    enabled = enabled && !isRunning,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .width(18.dp)
                                .height(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(runningButtonLabel)
                    } else {
                        Text(idleButtonLabel)
                    }
                }
            },
        )
    }
}

@Composable
private fun ExportSummarySection(
    rows: List<Pair<String, String>>,
) {
    SectionSurface {
        Text(
            text = stringResource(R.string.functions_last_exported),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        rows.forEach { (label, value) ->
            OptionalRow(label, value)
        }
    }
}

@Composable
private fun ExportProgressSection(
    current: Int,
    total: Int,
) {
    SectionSurface {
        Text(
            text = stringResource(R.string.functions_progress_processed, current, total),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (total > 0) {
            LinearProgressIndicator(
                progress = { (current.toFloat() / total.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun CsvExportFormat.labelRes(): Int = when (this) {
    CsvExportFormat.SYSTEM_DEFAULT -> R.string.csv_export_format_system_default_label
    CsvExportFormat.EXCEL_DE -> R.string.csv_export_format_excel_de_label
    CsvExportFormat.STANDARD_INTERNATIONAL -> R.string.csv_export_format_standard_international_label
}
