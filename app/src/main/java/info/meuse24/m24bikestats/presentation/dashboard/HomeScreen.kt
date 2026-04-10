package info.meuse24.m24bikestats.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import info.meuse24.m24bikestats.R
import info.meuse24.m24bikestats.domain.model.SmartSystemCloudSyncPhase
import info.meuse24.m24bikestats.presentation.theme.DesignTokens

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onRefresh: () -> Unit,
    onCancelRefresh: () -> Unit,
    onNavigateToActivityDetail: (String) -> Unit,
    onNavigateToActivityTrack: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val latestActivity = remember(uiState.allActivities) {
        uiState.allActivities.maxByOrNull { it.startedAtEpochMillis ?: Long.MIN_VALUE }
    }
    val primaryBike = uiState.bikes.firstOrNull()
    val syncProgress = remember(
        uiState.isSyncingCloudData,
        uiState.syncPhase,
        uiState.syncPhaseLabel,
        uiState.syncLoadedActivityCount,
        uiState.syncTotalActivityCount,
    ) {
        uiState.toSyncProgressUi(
            onCancelRefresh = onCancelRefresh,
        )
    }
    val isInitialSetup = uiState.isInitialLoading && uiState.allActivities.isEmpty() && uiState.bikes.isEmpty()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = DesignTokens.ScreenHorizontalPadding,
            vertical = DesignTokens.ScreenVerticalPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SectionSpacing),
    ) {
        item {
            DashboardPageContainer {
                HomeStatusHeroCard(
                    uiState = uiState,
                    isInitialSetup = isInitialSetup,
                    onRefresh = onRefresh,
                )
            }
        }

        if (isInitialSetup) {
            item {
                DashboardPageContainer {
                    HomeEmptyStateCard(
                        title = stringResource(R.string.home_initial_sync_title),
                        description = stringResource(R.string.home_initial_sync_text).takeIf {
                            uiState.showExplanationTexts
                        },
                    )
                }
            }
        }

        uiState.dataStatus?.let { dataStatus ->
            item {
                DashboardPageContainer {
                    HomeStatusMetricGrid(dataStatus = dataStatus)
                }
            }
        }

        item {
            DashboardPageContainer {
                HomeSyncMetaCard(
                    uiState = uiState,
                    progress = syncProgress,
                )
            }
        }

        item {
            DashboardPageContainer {
                HomeSection(title = stringResource(R.string.home_latest_tour)) {
                    if (latestActivity != null) {
                        ActivityCard(
                            activity = latestActivity,
                            onClick = { onNavigateToActivityDetail(latestActivity.id) },
                            onMapClick = { onNavigateToActivityTrack(latestActivity.id) },
                            onShareClick = { shareActivitySummary(context, latestActivity) },
                            showActionLabels = uiState.showExplanationTexts,
                        )
                    } else {
                        HomeEmptyStateCard(
                            title = stringResource(R.string.home_no_tours_title),
                            description = stringResource(R.string.home_no_tours_text).takeIf {
                                uiState.showExplanationTexts
                            },
                        )
                    }
                }
            }
        }

        item {
            DashboardPageContainer {
                HomeSection(title = stringResource(R.string.home_bike_status)) {
                    DashboardSectionCard {
                        if (primaryBike != null) {
                            Text(
                                text = primaryBike.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            primaryBike.subtitle?.let { subtitle ->
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            DashboardMetricGrid(
                                items = buildList {
                                    primaryBike.odometerLabel?.let {
                                        add(
                                            DashboardMetricTileModel(
                                                label = stringResource(R.string.home_odometer),
                                                value = it,
                                                tone = DashboardMetricTone.Informative,
                                            )
                                        )
                                    }
                                    primaryBike.assistSpeedLabel?.let {
                                        add(DashboardMetricTileModel(stringResource(R.string.home_assist_limit), it))
                                    }
                                    primaryBike.walkAssistLabel?.let {
                                        add(DashboardMetricTileModel(stringResource(R.string.home_walk_assist), it))
                                    }
                                    primaryBike.powerOnSummary?.let {
                                        add(DashboardMetricTileModel(stringResource(R.string.home_usage), it))
                                    }
                                    primaryBike.batterySummary?.let {
                                        add(
                                            DashboardMetricTileModel(
                                                label = stringResource(R.string.home_battery),
                                                value = it,
                                                tone = DashboardMetricTone.Positive,
                                            )
                                        )
                                    }
                                },
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.home_no_bike_data),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        item {
            DashboardPageContainer {
                HomeSection(title = stringResource(R.string.home_recent_exports)) {
                    DashboardSectionCard {
                        DashboardMetaRow(
                            label = stringResource(R.string.home_export_activities_csv),
                            value = uiState.lastActivitiesCsvExport?.fileName ?: stringResource(R.string.home_export_none),
                        )
                        DashboardMetaRow(
                            label = stringResource(R.string.home_export_details_csv),
                            value = uiState.lastActivityDetailsCsvExport?.fileName ?: stringResource(R.string.home_export_none),
                        )
                        DashboardMetaRow(
                            label = stringResource(R.string.home_export_pdf),
                            value = uiState.lastPdfExport?.fileName ?: stringResource(R.string.home_export_none),
                        )
                        DashboardMetaRow(
                            label = stringResource(R.string.home_export_last_time),
                            value = uiState.lastPdfExport?.exportedAtLabel
                                ?: uiState.lastActivityDetailsCsvExport?.exportedAtLabel
                                ?: uiState.lastActivitiesCsvExport?.exportedAtLabel
                                ?: stringResource(R.string.home_export_none),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeStatusHeroCard(
    uiState: HomeUiState,
    isInitialSetup: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dataStatus = uiState.dataStatus
    val headlineRes = when {
        isInitialSetup -> R.string.home_initial_sync_title
        dataStatus != null -> dataStatus.statusHeadlineRes
        else -> R.string.home_data_status_loading
    }
    val summaryText = when {
        isInitialSetup -> stringResource(R.string.home_initial_sync_text)
        uiState.shouldShowStatusSummary -> dataStatus?.statusSummary ?: stringResource(R.string.home_data_status_loading)
        else -> null
    }
    DashboardHeroSurface(
        modifier = modifier,
        accentTone = dataStatus.toBadgeTone(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.home_data_status_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(headlineRes),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            summaryText?.let { statusSummary ->
                Text(
                    text = statusSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (dataStatus != null && !isInitialSetup) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    dataStatus?.coveredPeriodLabel?.let { coveredPeriod ->
                        Text(
                            text = stringResource(R.string.home_data_status_period_value, coveredPeriod),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } ?: Spacer(modifier = Modifier.weight(1f))
                    DashboardStatusBadge(
                        label = dataStatus.statusLabel,
                        tone = dataStatus.toBadgeTone(),
                    )
                }
            }
        }
        HomePrimaryActionBar(
            label = stringResource(dataStatus?.primaryActionLabelRes ?: R.string.home_refresh_button),
            enabled = uiState.canStartSync,
            isRunning = uiState.isSyncingCloudData,
            showLabel = uiState.showExplanationTexts,
            onClick = onRefresh,
        )
    }
}

@Composable
private fun HomeStatusMetricGrid(
    dataStatus: DataStatusUiModel,
    modifier: Modifier = Modifier,
) {
    DashboardMetricGrid(
        modifier = modifier,
        items = listOf(
            DashboardMetricTileModel(
                label = stringResource(R.string.home_data_status_activities),
                value = dataStatus.cachedActivityCount.toString(),
                tone = DashboardMetricTone.Informative,
            ),
            DashboardMetricTileModel(
                label = stringResource(R.string.home_data_status_details),
                value = "${dataStatus.detailedActivityCount} / ${dataStatus.cachedActivityCount}",
                supportingText = stringResource(
                    R.string.home_data_status_coverage_support,
                    dataStatus.detailCoveragePercent,
                ),
                tone = if (dataStatus.isComplete) {
                    DashboardMetricTone.Positive
                } else {
                    DashboardMetricTone.Informative
                },
            ),
            DashboardMetricTileModel(
                label = stringResource(R.string.home_data_status_missing),
                value = dataStatus.missingDetailCount.toString(),
                tone = if (dataStatus.hasMissingDetails) {
                    DashboardMetricTone.Warning
                } else {
                    DashboardMetricTone.Positive
                },
            ),
            DashboardMetricTileModel(
                label = stringResource(R.string.home_data_status_gps_points),
                value = dataStatus.gpsPointCount.toString(),
            ),
        ),
    )
}

@Composable
private fun HomeSyncMetaCard(
    uiState: HomeUiState,
    progress: HomeSyncProgressUi?,
    modifier: Modifier = Modifier,
) {
    DashboardSectionCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.home_sync_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        stringResource(R.string.home_sync_subtitle).takeIf { uiState.showExplanationTexts }?.let { subtitle ->
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        uiState.lastCloudSyncSummary?.let { summary ->
            DashboardMetaRow(
                label = stringResource(R.string.home_sync_last_sync),
                value = summary.syncedAtLabel,
            )
            DashboardMetricGrid(
                items = listOf(
                    DashboardMetricTileModel(
                        label = stringResource(R.string.home_sync_activities),
                        value = summary.activityCount.toString(),
                    ),
                    DashboardMetricTileModel(
                        label = stringResource(R.string.home_sync_bikes),
                        value = summary.bikeCount.toString(),
                    ),
                ),
            )
        }

        if (uiState.hasSyncMetadata) {
            SectionSurface {
                OptionalRow(stringResource(R.string.home_data_status_last_activity_sync), uiState.dataStatus?.lastActivitySyncLabel)
                OptionalRow(stringResource(R.string.home_data_status_last_detail_sync), uiState.dataStatus?.lastDetailSyncLabel)
                OptionalRow(stringResource(R.string.home_data_status_last_bike_sync), uiState.dataStatus?.lastBikeSyncLabel)
            }
        } else if (progress == null && uiState.showExplanationTexts) {
            Text(
                text = stringResource(R.string.home_sync_meta_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        progress?.let {
            HomeSyncProgressCard(
                progress = it,
                showButtonLabel = uiState.showExplanationTexts,
            )
        }
    }
}

@Composable
private fun HomeSyncProgressCard(
    progress: HomeSyncProgressUi,
    showButtonLabel: Boolean,
    modifier: Modifier = Modifier,
) {
    val cancelLabel = stringResource(R.string.home_sync_cancel_button)
    DashboardSectionCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        progress.phaseLabel?.let { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = if (progress.totalCount > 0) {
                stringResource(R.string.home_sync_progress, progress.loadedCount, progress.totalCount)
            } else {
                stringResource(R.string.home_sync_starting)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { progress.overallProgress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
            onClick = progress.onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (showButtonLabel) {
                        Modifier
                    } else {
                        Modifier.semantics { contentDescription = cancelLabel }
                    }
                ),
        ) {
            DashboardButtonContent(
                label = cancelLabel,
                showLabel = showButtonLabel,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun HomePrimaryActionBar(
    label: String,
    enabled: Boolean,
    isRunning: Boolean,
    showLabel: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HomeActionButton(
        onClick = onClick,
        enabled = enabled,
        label = if (isRunning) {
            stringResource(R.string.home_refresh_running)
        } else {
            label
        },
        icon = {
            if (isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
        showLabel = showLabel,
        modifier = modifier.fillMaxWidth(),
        emphasis = HomeActionButtonEmphasis.Primary,
    )
}

@Composable
private fun HomeSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HomeSectionHeader(title = title)
        content()
    }
}

@Composable
private fun HomeSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        modifier = modifier,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun HomeEmptyStateCard(
    title: String,
    description: String?,
    modifier: Modifier = Modifier,
) {
    DashboardSectionCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HomeActionButton(
    onClick: () -> Unit,
    enabled: Boolean,
    label: String,
    icon: @Composable () -> Unit,
    showLabel: Boolean,
    modifier: Modifier = Modifier,
    emphasis: HomeActionButtonEmphasis = HomeActionButtonEmphasis.Secondary,
) {
    val buttonModifier = modifier
        .heightIn(min = 52.dp)
        .then(
            if (showLabel) {
                Modifier
            } else {
                Modifier.semantics { contentDescription = label }
            }
        )
    when (emphasis) {
        HomeActionButtonEmphasis.Primary -> {
            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = buttonModifier,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                ),
                contentPadding = ButtonDefaults.ContentPadding,
            ) {
                DashboardButtonContent(
                    label = label,
                    icon = icon,
                    showLabel = showLabel,
                )
            }
        }

        HomeActionButtonEmphasis.Secondary -> {
            FilledTonalButton(
                onClick = onClick,
                enabled = enabled,
                modifier = buttonModifier,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                ),
                contentPadding = ButtonDefaults.ContentPadding,
            ) {
                DashboardButtonContent(
                    label = label,
                    icon = icon,
                    showLabel = showLabel,
                )
            }
        }
    }
}

private enum class HomeActionButtonEmphasis {
    Primary,
    Secondary,
}

private data class HomeSyncProgressUi(
    val phaseLabel: String?,
    val loadedCount: Int,
    val totalCount: Int,
    val overallProgress: Float,
    val onCancel: () -> Unit,
)

private fun DataStatusUiModel?.toBadgeTone(): DashboardStatusBadgeTone = when (this?.statusTone) {
    DataStatusTone.EMPTY,
    null,
    -> DashboardStatusBadgeTone.Neutral

    DataStatusTone.PARTIAL -> DashboardStatusBadgeTone.Warning
    DataStatusTone.COMPLETE -> DashboardStatusBadgeTone.Positive
}

private fun HomeUiState.toSyncProgressUi(
    onCancelRefresh: () -> Unit,
): HomeSyncProgressUi? = when {
    isSyncingCloudData -> {
        val phaseProgress = if (syncTotalActivityCount > 0) {
            syncLoadedActivityCount.toFloat() / syncTotalActivityCount.toFloat()
        } else {
            0f
        }
        val overallProgress = when (syncPhase) {
            SmartSystemCloudSyncPhase.BIKES -> phaseProgress / 3f
            SmartSystemCloudSyncPhase.ACTIVITIES -> (1f + phaseProgress) / 3f
            SmartSystemCloudSyncPhase.ACTIVITY_DETAILS -> (2f + phaseProgress) / 3f
            null -> 0f
        }
        HomeSyncProgressUi(
            phaseLabel = syncPhaseLabel,
            loadedCount = syncLoadedActivityCount,
            totalCount = syncTotalActivityCount,
            overallProgress = overallProgress,
            onCancel = onCancelRefresh,
        )
    }

    else -> null
}
