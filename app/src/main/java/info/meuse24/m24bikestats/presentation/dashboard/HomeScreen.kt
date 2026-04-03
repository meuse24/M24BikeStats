package info.meuse24.m24bikestats.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import info.meuse24.m24bikestats.R

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onSyncCloudData: () -> Unit,
    onNavigateToActivities: () -> Unit,
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
                eyebrow = "",
                title = "",
                subtitle = stringResource(R.string.home_hero_subtitle),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    SummaryChipRow(
                        summary = listOf(
                            stringResource(R.string.home_metric_tours) to uiState.loadedActivityCount.toString(),
                            stringResource(R.string.home_metric_bikes) to uiState.bikes.size.toString(),
                            stringResource(R.string.home_metric_visible) to uiState.visibleActivityCount.toString(),
                        ),
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
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_sync_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.home_sync_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    uiState.lastCloudSyncSummary?.let { summary ->
                        SectionSurface {
                            OptionalRow(stringResource(R.string.home_sync_last_sync), summary.syncedAtLabel)
                            OptionalRow(stringResource(R.string.home_sync_activities), summary.activityCount.toString())
                            OptionalRow(stringResource(R.string.home_sync_bikes), summary.bikeCount.toString())
                        }
                    }
                    if (uiState.isSyncingCloudData) {
                        val totalCount = uiState.syncTotalActivityCount
                        val loadedCount = uiState.syncLoadedActivityCount
                        val progress = if (totalCount > 0) {
                            loadedCount.toFloat() / totalCount.toFloat()
                        } else {
                            0f
                        }
                        Text(
                            text = if (totalCount > 0) {
                                stringResource(R.string.home_sync_progress, loadedCount, totalCount)
                            } else {
                                stringResource(R.string.home_sync_starting)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Button(
                        onClick = onSyncCloudData,
                        enabled = !uiState.isSyncingCloudData &&
                            !uiState.isRefreshing &&
                            !uiState.isInitialLoading &&
                            !uiState.isExportingActivitiesCsv &&
                            !uiState.isExportingActivityDetailsCsv,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (uiState.isSyncingCloudData) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height(18.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(stringResource(R.string.home_sync_running))
                        } else {
                            Text(stringResource(R.string.home_sync_button))
                        }
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
                        text = stringResource(R.string.home_latest_tour),
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
                            text = stringResource(R.string.home_no_tours_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.home_no_tours_text),
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
                        text = stringResource(R.string.home_bike_status),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    primaryBike?.let { bike ->
                        SectionSurface {
                            OptionalRow(stringResource(R.string.nav_bike), bike.title)
                            OptionalRow(stringResource(R.string.home_head_unit), bike.subtitle)
                            OptionalRow(stringResource(R.string.home_odometer), bike.odometerLabel)
                            OptionalRow(stringResource(R.string.home_assist_limit), bike.assistSpeedLabel)
                            OptionalRow(stringResource(R.string.home_battery), bike.batterySummary)
                        }
                    } ?: Text(
                        text = stringResource(R.string.home_no_bike_data),
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
                        text = stringResource(R.string.home_recent_exports),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    SectionSurface {
                        OptionalRow(
                            stringResource(R.string.home_export_activities_csv),
                            uiState.lastActivitiesCsvExport?.fileName ?: stringResource(R.string.home_export_none),
                        )
                        OptionalRow(
                            stringResource(R.string.home_export_details_csv),
                            uiState.lastActivityDetailsCsvExport?.fileName ?: stringResource(R.string.home_export_none),
                        )
                        OptionalRow(
                            stringResource(R.string.home_export_last_time),
                            uiState.lastActivityDetailsCsvExport?.exportedAtLabel
                                ?: uiState.lastActivitiesCsvExport?.exportedAtLabel
                                ?: stringResource(R.string.home_export_none),
                        )
                    }
                }
            }
        }
    }
}
