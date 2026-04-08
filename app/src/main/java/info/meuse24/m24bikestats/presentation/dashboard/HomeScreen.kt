package info.meuse24.m24bikestats.presentation.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
    onLoadMissingActivityDetails: () -> Unit,
    onRefreshStaleActivityDetails: () -> Unit,
    onCancelPendingActivityDetailsSync: () -> Unit,
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
            DataStatusAndSyncCard(
                uiState = uiState,
                onSyncCloudData = onSyncCloudData,
                onCancelSyncCloudData = onCancelSyncCloudData,
                onLoadMissingActivityDetails = onLoadMissingActivityDetails,
                onRefreshStaleActivityDetails = onRefreshStaleActivityDetails,
                onCancelPendingActivityDetailsSync = onCancelPendingActivityDetailsSync,
            )
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
private fun DataStatusAndSyncCard(
    uiState: HomeUiState,
    onSyncCloudData: () -> Unit,
    onCancelSyncCloudData: () -> Unit,
    onLoadMissingActivityDetails: () -> Unit,
    onRefreshStaleActivityDetails: () -> Unit,
    onCancelPendingActivityDetailsSync: () -> Unit,
) {
    val dataStatus = uiState.dataStatus
    val hasSyncMetadata = dataStatus?.lastActivitySyncLabel != null ||
        dataStatus?.lastDetailSyncLabel != null ||
        dataStatus?.lastBikeSyncLabel != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    ),
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_data_status_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = dataStatus?.statusSummary ?: stringResource(R.string.home_data_status_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                    )
                }
                dataStatus?.let { status ->
                    DataStatusBadge(
                        label = status.statusLabel,
                        tone = status.statusTone,
                    )
                }
            }

            dataStatus?.coveredPeriodLabel?.let { coveredPeriodLabel ->
                Text(
                    text = stringResource(R.string.home_data_status_period_value, coveredPeriodLabel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                )
            }

            dataStatus?.let { status ->
                SummaryChipRow(
                    summary = listOf(
                        stringResource(R.string.home_data_status_activities) to status.cachedActivityCount.toString(),
                        stringResource(R.string.home_data_status_details) to status.detailCoverageLabel,
                        stringResource(R.string.home_data_status_missing) to status.missingDetailCount.toString(),
                        stringResource(R.string.home_data_status_stale) to status.staleDetailCount.toString(),
                        stringResource(R.string.home_data_status_gps_points) to status.gpsPointCount.toString(),
                    ),
                    itemContent = { label, value ->
                        CompactMetricPill(label = label, value = value)
                    },
                )
            }

            if (hasSyncMetadata) {
                SectionSurface {
                    OptionalRow(stringResource(R.string.home_data_status_last_activity_sync), dataStatus?.lastActivitySyncLabel)
                    OptionalRow(stringResource(R.string.home_data_status_last_detail_sync), dataStatus?.lastDetailSyncLabel)
                    OptionalRow(stringResource(R.string.home_data_status_last_bike_sync), dataStatus?.lastBikeSyncLabel)
                }
            }

            when {
                uiState.isSyncingPendingActivityDetails -> {
                    SyncProgressSection(
                        phaseLabel = uiState.pendingActivityDetailSyncLabel,
                        loadedCount = uiState.pendingActivityDetailSyncLoadedCount,
                        totalCount = uiState.pendingActivityDetailSyncTotalCount,
                        overallProgress = if (uiState.pendingActivityDetailSyncTotalCount > 0) {
                            uiState.pendingActivityDetailSyncLoadedCount.toFloat() /
                                uiState.pendingActivityDetailSyncTotalCount.toFloat()
                        } else {
                            0f
                        },
                        onCancel = onCancelPendingActivityDetailsSync,
                    )
                }

                uiState.isSyncingCloudData -> {
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
                    SyncProgressSection(
                        phaseLabel = uiState.syncPhaseLabel,
                        loadedCount = loadedCount,
                        totalCount = totalCount,
                        overallProgress = overallProgress,
                        onCancel = onCancelSyncCloudData,
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when {
                    (dataStatus?.missingDetailCount ?: 0) > 0 && (dataStatus?.staleDetailCount ?: 0) > 0 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = onLoadMissingActivityDetails,
                                enabled = uiState.canStartSync,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(R.string.home_data_status_action_missing))
                            }
                            OutlinedButton(
                                onClick = onRefreshStaleActivityDetails,
                                enabled = uiState.canStartSync,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(R.string.home_data_status_action_stale))
                            }
                        }
                    }

                    (dataStatus?.missingDetailCount ?: 0) > 0 -> {
                        OutlinedButton(
                            onClick = onLoadMissingActivityDetails,
                            enabled = uiState.canStartSync,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.home_data_status_action_missing))
                        }
                    }

                    (dataStatus?.staleDetailCount ?: 0) > 0 -> {
                        OutlinedButton(
                            onClick = onRefreshStaleActivityDetails,
                            enabled = uiState.canStartSync,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.home_data_status_action_stale))
                        }
                    }
                }

                Button(
                    onClick = onSyncCloudData,
                    enabled = uiState.canStartSync,
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
}

@Composable
private fun SyncProgressSection(
    phaseLabel: String?,
    loadedCount: Int,
    totalCount: Int,
    overallProgress: Float,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        phaseLabel?.let { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Text(
            text = if (totalCount > 0) {
                stringResource(R.string.home_sync_progress, loadedCount, totalCount)
            } else {
                stringResource(R.string.home_sync_starting)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
        )
        LinearProgressIndicator(
            progress = { overallProgress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.home_sync_cancel_button))
        }
    }
}

@Composable
private fun DataStatusBadge(
    label: String,
    tone: DataStatusTone,
) {
    val containerColor = when (tone) {
        DataStatusTone.EMPTY -> MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
        DataStatusTone.PARTIAL -> MaterialTheme.colorScheme.tertiaryContainer
        DataStatusTone.STALE -> MaterialTheme.colorScheme.errorContainer
        DataStatusTone.COMPLETE -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when (tone) {
        DataStatusTone.EMPTY -> MaterialTheme.colorScheme.onPrimaryContainer
        DataStatusTone.PARTIAL -> MaterialTheme.colorScheme.onTertiaryContainer
        DataStatusTone.STALE -> MaterialTheme.colorScheme.onErrorContainer
        DataStatusTone.COMPLETE -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
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
        border = BorderStroke(
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
