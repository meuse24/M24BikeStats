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
import info.meuse24.m24bikestats.domain.model.CsvSeparator

@Composable
fun FunctionsScreen(
    uiState: DashboardUiState,
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
        context.startActivity(Intent.createChooser(shareIntent, "CSV exportieren"))
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
        context.startActivity(Intent.createChooser(shareIntent, "Detail-CSV exportieren"))
        onActivityDetailsCsvExportHandled()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            HeroCard(
                eyebrow = "Funktionen",
                title = "Datenexport",
                subtitle = "Aktivitätenlisten und Detailpunkte als teilbare CSV-Dateien exportieren.",
            ) {
                SummaryChipRow(
                    listOf(
                        "Geladen" to uiState.loadedActivityCount.toString(),
                        "Gesamt" to uiState.activityTotalCount.toString(),
                        "Sichtbar" to uiState.visibleActivityCount.toString(),
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
                        text = "Aktivitäten als CSV exportieren",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Die Funktion ruft alle Seiten der Aktivitätenliste ab und erstellt daraus eine teilbare CSV-Datei mit den bekannten Metriken.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SectionSurface {
                        OptionalRow("CSV-Trennzeichen", stringResource(uiState.csvSeparator.labelRes()))
                    }
                    uiState.lastActivitiesCsvExport?.let { exportSummary ->
                        SectionSurface {
                            Text(
                                text = "Zuletzt exportiert",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            OptionalRow("Datei", exportSummary.fileName)
                            OptionalRow("Aktivitäten", exportSummary.activityCount.toString())
                            OptionalRow("Zeitpunkt", exportSummary.exportedAtLabel)
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
                            text = "$loadedCount von $totalCount Aktivitäten geladen",
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
                            Text("Export läuft")
                        } else {
                            Text("CSV exportieren")
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
                        text = "Sichtbare Detailpunkte als CSV exportieren",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Verwendet den aktuell sichtbaren Aktivitätssatz und exportiert alle Detailpunkte aus Cache und bei Bedarf per Live-Nachladen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SectionSurface {
                        OptionalRow("CSV-Trennzeichen", stringResource(uiState.csvSeparator.labelRes()))
                        OptionalRow("Sichtbare Aktivitäten", uiState.visibleActivityCount.toString())
                        OptionalRow("Geladene Aktivitäten", uiState.loadedActivityCount.toString())
                    }
                    uiState.lastActivityDetailsCsvExport?.let { exportSummary ->
                        SectionSurface {
                            Text(
                                text = "Zuletzt exportiert",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            OptionalRow("Datei", exportSummary.fileName)
                            OptionalRow("Aktivitäten", exportSummary.activityCount.toString())
                            OptionalRow("Detailpunkte", exportSummary.detailPointCount.toString())
                            OptionalRow("Zeitpunkt", exportSummary.exportedAtLabel)
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
                            text = "$loadedCount von $totalCount Aktivitäten geladen",
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
                            Text("Detail-Export läuft")
                        } else {
                            Text("Detail-CSV exportieren")
                        }
                    }
                }
            }
        }
    }
}

private fun CsvSeparator.labelRes(): Int = when (this) {
    CsvSeparator.SEMICOLON -> R.string.csv_separator_semicolon_label
    CsvSeparator.COMMA -> R.string.csv_separator_comma_label
}
