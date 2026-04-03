package info.meuse24.m24bikestats.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    uiState: DashboardUiState,
    onNavigateToActivities: () -> Unit,
    onNavigateToBike: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestActivity = remember(uiState.allActivities) {
        uiState.allActivities.maxByOrNull { it.startedAtEpochMillis ?: Long.MIN_VALUE }
    }
    val primaryBike = uiState.bikes.firstOrNull()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            HeroCard(
                eyebrow = "Home",
                title = "Übersicht für Touren, Bike und Exporte",
                subtitle = "Die Startseite bündelt den letzten Fahrtdatensatz, Bike-Status und die letzten Exportergebnisse.",
            ) {
                SummaryChipRow(
                    listOf(
                        "Touren" to uiState.loadedActivityCount.toString(),
                        "Bikes" to uiState.bikes.size.toString(),
                        "Sichtbar" to uiState.visibleActivityCount.toString(),
                    )
                )
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
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "Schnellzugriffe",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Button(
                        onClick = onNavigateToActivities,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Zu den Aktivitäten")
                    }
                    OutlinedButton(
                        onClick = { primaryBike?.let { onNavigateToBike(it.id) } },
                        enabled = primaryBike != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (primaryBike != null) "Bike öffnen" else "Kein Bike verfügbar")
                    }
                }
            }
        }
        item {
            if (latestActivity != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Letzte Tour",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    ActivityCard(
                        activity = latestActivity,
                        onClick = onNavigateToActivities,
                    )
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Noch keine Tourdaten verfügbar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Nach dem ersten erfolgreichen Sync erscheinen hier die zuletzt geladene Aktivität und Schnellzugriffe.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Bike-Status",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    primaryBike?.let { bike ->
                        SectionSurface {
                            OptionalRow("Bike", bike.title)
                            OptionalRow("Head Unit", bike.subtitle)
                            OptionalRow("Kilometerstand", bike.odometerLabel)
                            OptionalRow("Assist-Limit", bike.assistSpeedLabel)
                            OptionalRow("Batterie", bike.batterySummary)
                        }
                    } ?: Text(
                        text = "Noch keine Bike-Daten im Cache.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Letzte Exporte",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    SectionSurface {
                        OptionalRow(
                            "Aktivitäten-CSV",
                            uiState.lastActivitiesCsvExport?.fileName ?: "Noch kein Export",
                        )
                        OptionalRow(
                            "Details-CSV",
                            uiState.lastActivityDetailsCsvExport?.fileName ?: "Noch kein Export",
                        )
                        OptionalRow(
                            "Zuletzt exportiert",
                            uiState.lastActivityDetailsCsvExport?.exportedAtLabel
                                ?: uiState.lastActivitiesCsvExport?.exportedAtLabel
                                ?: "Noch kein Export",
                        )
                    }
                }
            }
        }
    }
}
