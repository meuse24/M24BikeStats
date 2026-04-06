package info.meuse24.m24bikestats.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import info.meuse24.m24bikestats.R
import info.meuse24.m24bikestats.domain.model.SmartSystemCloudSyncPhase

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onSyncCloudData: () -> Unit,
    onCancelSyncCloudData: () -> Unit,
    onNavigateToActivityDetail: (String) -> Unit,
    onNavigateToActivityTrack: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
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
                            stringResource(R.string.home_metric_details) to uiState.cachedDetailActivityCount.toString(),
                            stringResource(R.string.home_metric_gpx) to uiState.cachedGpsPointCount.toString(),
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
                        val phaseProgress = if (totalCount > 0) {
                            loadedCount.toFloat() / totalCount.toFloat()
                        } else {
                            0f
                        }
                        val overallProgress = when (uiState.syncPhase) {
                            SmartSystemCloudSyncPhase.BIKES -> phaseProgress / 3f
                            SmartSystemCloudSyncPhase.ACTIVITIES -> (1f + phaseProgress) / 3f
                            SmartSystemCloudSyncPhase.ACTIVITY_DETAILS -> (2f + phaseProgress) / 3f
                            null -> 0f
                        }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            uiState.syncPhaseLabel?.let { phaseLabel ->
                                Text(
                                    text = phaseLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Text(
                                text = if (totalCount > 0) {
                                    stringResource(R.string.home_sync_progress, loadedCount, totalCount)
                                } else {
                                    stringResource(R.string.home_sync_starting)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { overallProgress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedButton(
                            onClick = onCancelSyncCloudData,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.home_sync_cancel_button))
                        }
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
                        onClick = { onNavigateToActivityDetail(latestActivity.id) },
                        onMapClick = { onNavigateToActivityTrack(latestActivity.id) },
                        onShareClick = { shareActivitySummary(context, latestActivity) },
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
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_bike_status),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                HomeSummaryCard {
                    primaryBike?.let { bike ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = bike.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            bike.subtitle?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            HomeMetricGrid(
                                items = buildList {
                                    bike.odometerLabel?.let { add(stringResource(R.string.home_odometer) to it) }
                                    bike.assistSpeedLabel?.let { add(stringResource(R.string.home_assist_limit) to it) }
                                    bike.walkAssistLabel?.let { add(stringResource(R.string.home_walk_assist) to it) }
                                    bike.powerOnSummary?.let { add(stringResource(R.string.home_usage) to it) }
                                    bike.batterySummary?.let { add(stringResource(R.string.home_battery) to it) }
                                },
                            )
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
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_recent_exports),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                HomeSummaryCard {
                    HomeMetricGrid(
                        items = listOf(
                            stringResource(R.string.home_export_activities_csv) to (
                                uiState.lastActivitiesCsvExport?.fileName ?: stringResource(R.string.home_export_none)
                            ),
                            stringResource(R.string.home_export_details_csv) to (
                                uiState.lastActivityDetailsCsvExport?.fileName ?: stringResource(R.string.home_export_none)
                            ),
                            stringResource(R.string.home_export_pdf) to (
                                uiState.lastPdfExport?.fileName ?: stringResource(R.string.home_export_none)
                            ),
                            stringResource(R.string.home_export_last_time) to (
                                uiState.lastPdfExport?.exportedAtLabel
                                    ?: uiState.lastActivityDetailsCsvExport?.exportedAtLabel
                                    ?: uiState.lastActivitiesCsvExport?.exportedAtLabel
                                    ?: stringResource(R.string.home_export_none)
                            ),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeSummaryCard(
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun HomeMetricGrid(
    items: List<Pair<String, String>>,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowItems.forEach { (label, value) ->
                    HomeMetricTile(
                        label = label,
                        value = value,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HomeMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
