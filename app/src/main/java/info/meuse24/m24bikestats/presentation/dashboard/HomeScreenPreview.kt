package info.meuse24.m24bikestats.presentation.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import info.meuse24.m24bikestats.domain.model.DisplayMode
import info.meuse24.m24bikestats.domain.model.SmartSystemCloudSyncPhase
import info.meuse24.m24bikestats.presentation.theme.M24BikeStatsTheme

@Preview(name = "Home Complete", showBackground = true, widthDp = 412, heightDp = 1280)
@Composable
private fun HomeScreenPreviewComplete() {
    PreviewHomeScreen(
        uiState = previewHomeUiState(
            dataStatus = previewCompleteStatus(),
            lastCloudSyncSummary = CloudSyncSummaryUiModel(
                activityCount = 184,
                bikeCount = 2,
                syncedAtLabel = "09.04.2026 06:10",
            ),
        ),
        displayMode = DisplayMode.LIGHT,
    )
}

@Preview(name = "Home Incomplete", showBackground = true, widthDp = 412, heightDp = 1280)
@Composable
private fun HomeScreenPreviewIncomplete() {
    PreviewHomeScreen(
        uiState = previewHomeUiState(
            dataStatus = previewPartialStatus(),
            lastCloudSyncSummary = CloudSyncSummaryUiModel(
                activityCount = 182,
                bikeCount = 2,
                syncedAtLabel = "08.04.2026 20:14",
            ),
        ),
        displayMode = DisplayMode.LIGHT,
    )
}

@Preview(name = "Home Sync Dark", showBackground = true, widthDp = 412, heightDp = 1280)
@Composable
private fun HomeScreenPreviewSyncDark() {
    PreviewHomeScreen(
        uiState = previewHomeUiState(
            dataStatus = previewPartialStatus(),
            isSyncingCloudData = true,
            syncPhase = SmartSystemCloudSyncPhase.ACTIVITY_DETAILS,
            syncPhaseLabel = "Loading ride details",
            syncLoadedActivityCount = 74,
            syncTotalActivityCount = 120,
            canStartSync = false,
        ),
        displayMode = DisplayMode.DARK,
    )
}

@Preview(name = "Home Complete Dark", showBackground = true, widthDp = 412, heightDp = 1280)
@Composable
private fun HomeScreenPreviewCompleteDark() {
    PreviewHomeScreen(
        uiState = previewHomeUiState(
            dataStatus = previewCompleteStatus(),
            lastCloudSyncSummary = CloudSyncSummaryUiModel(
                activityCount = 184,
                bikeCount = 2,
                syncedAtLabel = "09.04.2026 06:10",
            ),
        ),
        displayMode = DisplayMode.DARK,
    )
}

@Preview(name = "Home Empty Large Text", showBackground = true, widthDp = 360, heightDp = 1280, fontScale = 1.8f)
@Composable
private fun HomeScreenPreviewEmptyLargeText() {
    PreviewHomeScreen(
        uiState = previewHomeUiState(
            allActivities = emptyList(),
            bikes = emptyList(),
            dataStatus = previewEmptyStatus(),
            lastCloudSyncSummary = null,
            lastActivitiesCsvExport = null,
            lastActivityDetailsCsvExport = null,
            lastPdfExport = null,
        ),
        displayMode = DisplayMode.LIGHT,
    )
}

@Composable
private fun PreviewHomeScreen(
    uiState: HomeUiState,
    displayMode: DisplayMode,
) {
    M24BikeStatsTheme(
        displayMode = displayMode,
        dynamicColor = false,
    ) {
        HomeScreen(
            uiState = uiState,
            onSyncCloudData = {},
            onCancelSyncCloudData = {},
            onLoadMissingActivityDetails = {},
            onRefreshStaleActivityDetails = {},
            onCancelPendingActivityDetailsSync = {},
            onNavigateToActivityDetail = {},
            onNavigateToActivityTrack = {},
        )
    }
}

private fun previewHomeUiState(
    allActivities: List<ActivityCardUiModel> = listOf(
        ActivityCardUiModel(
            id = "ride-1",
            title = "Morning gravel loop",
            startedAt = "2026-04-09T06:30:00Z",
            startedAtEpochMillis = 1_775_715_000_000,
            distanceMeters = 38200,
            durationSeconds = 5720,
            dateLabel = "09.04.2026 08:30",
            distanceLabel = "38.2 km",
            durationLabel = "1h 35m",
            speedLabel = "avg 24.1 km/h\nmax 41.3 km/h",
            powerLabel = "avg 188 W\nmax 412 W",
            elevationLabel = "+620 m / -618 m",
            caloriesLabel = "924 kcal",
        )
    ),
    bikes: List<BikeCardUiModel> = listOf(
        BikeCardUiModel(
            id = "bike-1",
            title = "Performance Line CX",
            subtitle = "Kiox 300",
            odometerLabel = "6,336.8 km",
            assistSpeedLabel = "27.4 km/h",
            walkAssistLabel = "active",
            powerOnSummary = "867 h total • 867 h support",
            assistModesSummary = "97 km | 59 km",
            batterySummary = "PowerTube 750 • 82 % health",
            bikePassSummary = "Frame no.: FRAME-123",
            shareText = "Performance Line CX",
        )
    ),
    dataStatus: DataStatusUiModel? = previewCompleteStatus(),
    isSyncingCloudData: Boolean = false,
    syncPhase: SmartSystemCloudSyncPhase? = null,
    syncPhaseLabel: String? = null,
    syncLoadedActivityCount: Int = 0,
    syncTotalActivityCount: Int = 0,
    isSyncingPendingActivityDetails: Boolean = false,
    pendingActivityDetailSyncLabel: String? = null,
    pendingActivityDetailSyncLoadedCount: Int = 0,
    pendingActivityDetailSyncTotalCount: Int = 0,
    canStartSync: Boolean = true,
    lastCloudSyncSummary: CloudSyncSummaryUiModel? = CloudSyncSummaryUiModel(
        activityCount = 184,
        bikeCount = 2,
        syncedAtLabel = "09.04.2026 06:10",
    ),
    lastActivitiesCsvExport: ActivitiesCsvExportSummaryUiModel? = ActivitiesCsvExportSummaryUiModel(
        fileName = "activities_2026-04-09.csv",
        activityCount = 184,
        exportedAtLabel = "09.04.2026 06:18",
    ),
    lastActivityDetailsCsvExport: ActivityDetailsCsvExportSummaryUiModel? = ActivityDetailsCsvExportSummaryUiModel(
        fileName = "activity_details_2026-04-09.csv",
        activityCount = 184,
        detailPointCount = 15_824,
        exportedAtLabel = "09.04.2026 06:20",
    ),
    lastPdfExport: PdfExportSummaryUiModel? = PdfExportSummaryUiModel(
        fileName = "m24_summary_2026-04-09.pdf",
        exportedAtLabel = "09.04.2026 06:22",
    ),
) = HomeUiState(
    allActivities = allActivities,
    bikes = bikes,
    loadedActivityCount = allActivities.size,
    visibleActivityCount = allActivities.size,
    isInitialLoading = false,
    isRefreshing = false,
    isSyncingCloudData = isSyncingCloudData,
    isExportingActivitiesCsv = false,
    isExportingActivityDetailsCsv = false,
    isExportingPdf = false,
    syncPhase = syncPhase,
    syncPhaseLabel = syncPhaseLabel,
    syncLoadedActivityCount = syncLoadedActivityCount,
    syncTotalActivityCount = syncTotalActivityCount,
    isSyncingPendingActivityDetails = isSyncingPendingActivityDetails,
    pendingActivityDetailSyncLabel = pendingActivityDetailSyncLabel,
    pendingActivityDetailSyncLoadedCount = pendingActivityDetailSyncLoadedCount,
    pendingActivityDetailSyncTotalCount = pendingActivityDetailSyncTotalCount,
    cachedDetailActivityCount = dataStatus?.detailedActivityCount ?: 0,
    cachedDetailPointCount = 15_824,
    cachedGpsPointCount = dataStatus?.gpsPointCount ?: 0,
    dataStatus = dataStatus,
    canStartSync = canStartSync,
    lastCloudSyncSummary = lastCloudSyncSummary,
    lastActivitiesCsvExport = lastActivitiesCsvExport,
    lastActivityDetailsCsvExport = lastActivityDetailsCsvExport,
    lastPdfExport = lastPdfExport,
    showExplanationTexts = true,
)

private fun previewCompleteStatus() = DataStatusUiModel(
    statusTone = DataStatusTone.COMPLETE,
    statusLabel = "Complete",
    statusSummary = "Ride cache complete.",
    coveredPeriodLabel = "01.02.2025 - 09.04.2026",
    cachedActivityCount = 184,
    detailedActivityCount = 184,
    detailCoverageLabel = "184 / 184 (100%)",
    missingDetailCount = 0,
    staleDetailCount = 0,
    gpsPointCount = 24_812,
    lastActivitySyncLabel = "09.04.2026 06:10",
    lastBikeSyncLabel = "09.04.2026 06:10",
    lastDetailSyncLabel = "09.04.2026 06:12",
)

private fun previewPartialStatus() = DataStatusUiModel(
    statusTone = DataStatusTone.PARTIAL,
    statusLabel = "Incomplete",
    statusSummary = "18 rides still need details. 6 detail caches can be refreshed.",
    coveredPeriodLabel = "01.02.2025 - 09.04.2026",
    cachedActivityCount = 184,
    detailedActivityCount = 166,
    detailCoverageLabel = "166 / 184 (90%)",
    missingDetailCount = 18,
    staleDetailCount = 6,
    gpsPointCount = 22_904,
    lastActivitySyncLabel = "09.04.2026 06:10",
    lastBikeSyncLabel = "09.04.2026 06:10",
    lastDetailSyncLabel = "08.04.2026 22:04",
)

private fun previewEmptyStatus() = DataStatusUiModel(
    statusTone = DataStatusTone.EMPTY,
    statusLabel = "Empty",
    statusSummary = "No ride data cached yet. Start a sync to fill the local cache.",
    coveredPeriodLabel = null,
    cachedActivityCount = 0,
    detailedActivityCount = 0,
    detailCoverageLabel = "0 / 0 (0%)",
    missingDetailCount = 0,
    staleDetailCount = 0,
    gpsPointCount = 0,
    lastActivitySyncLabel = null,
    lastBikeSyncLabel = null,
    lastDetailSyncLabel = null,
)
