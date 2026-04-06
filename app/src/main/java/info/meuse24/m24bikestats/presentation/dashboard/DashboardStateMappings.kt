package info.meuse24.m24bikestats.presentation.dashboard

fun DashboardUiState.toHomeUiState(): HomeUiState =
    HomeUiState(
        allActivities = allActivities,
        bikes = bikes,
        loadedActivityCount = loadedActivityCount,
        visibleActivityCount = visibleActivityCount,
        isInitialLoading = isInitialLoading,
        isRefreshing = isRefreshing,
        isSyncingCloudData = isSyncingCloudData,
        isExportingActivitiesCsv = isExportingActivitiesCsv,
        isExportingActivityDetailsCsv = isExportingActivityDetailsCsv,
        isExportingPdf = isExportingPdf,
        syncPhase = syncPhase,
        syncPhaseLabel = syncPhaseLabel,
        syncLoadedActivityCount = syncLoadedActivityCount,
        syncTotalActivityCount = syncTotalActivityCount,
        cachedDetailActivityCount = cachedDetailActivityCount,
        cachedDetailPointCount = cachedDetailPointCount,
        cachedGpsPointCount = cachedGpsPointCount,
        lastCloudSyncSummary = lastCloudSyncSummary,
        lastActivitiesCsvExport = lastActivitiesCsvExport,
        lastActivityDetailsCsvExport = lastActivityDetailsCsvExport,
    )

fun DashboardUiState.toActivitiesUiState(): ActivitiesUiState =
    ActivitiesUiState(
        activities = activities,
        activitySearchQuery = activitySearchQuery,
        activityDateRangeFilter = activityDateRangeFilter,
        activitySortOption = activitySortOption,
        visibleActivityCount = visibleActivityCount,
        loadedActivityCount = loadedActivityCount,
        activityTotalCount = activityTotalCount,
        isRefreshing = isRefreshing,
        isLoadingMoreActivities = isLoadingMoreActivities,
        canLoadMoreActivities = canLoadMoreActivities,
    )

fun DashboardUiState.toFunctionsUiState(): FunctionsUiState =
    FunctionsUiState(
        csvExportFormat = csvExportFormat,
        cloudSyncDetailMode = cloudSyncDetailMode,
        loadedActivityCount = loadedActivityCount,
        visibleActivityCount = visibleActivityCount,
        activityTotalCount = activityTotalCount,
        isInitialLoading = isInitialLoading,
        isRefreshing = isRefreshing,
        isExportingActivitiesCsv = isExportingActivitiesCsv,
        isExportingActivityDetailsCsv = isExportingActivityDetailsCsv,
        isExportingPdf = isExportingPdf,
        exportLoadedActivityCount = exportLoadedActivityCount,
        exportTotalActivityCount = exportTotalActivityCount,
        exportDetailedLoadedActivityCount = exportDetailedLoadedActivityCount,
        exportDetailedTotalActivityCount = exportDetailedTotalActivityCount,
        pendingActivitiesCsvExport = pendingActivitiesCsvExport,
        pendingActivityDetailsCsvExport = pendingActivityDetailsCsvExport,
        pendingPdfExport = pendingPdfExport,
        lastActivitiesCsvExport = lastActivitiesCsvExport,
        lastActivityDetailsCsvExport = lastActivityDetailsCsvExport,
        lastPdfExport = lastPdfExport,
    )

fun DashboardUiState.toBikeListUiState(): BikeListUiState =
    BikeListUiState(
        bikes = bikes,
        isRefreshing = isRefreshing,
        hasOidcCertificateInfo = hasOidcCertificateInfo,
    )

fun DashboardUiState.toActivityDetailScreenUiState(): ActivityDetailScreenUiState =
    ActivityDetailScreenUiState(
        selectedActivityDetail = selectedActivityDetail,
        selectedActivityId = selectedActivityId,
        isActivityDetailLoading = isActivityDetailLoading,
        isActivityDetailRefreshing = isActivityDetailRefreshing,
    )

fun DashboardUiState.toTrackUiState(): TrackUiState =
    TrackUiState(
        selectedActivityDetail = selectedActivityDetail,
        selectedActivityId = selectedActivityId,
        isActivityDetailLoading = isActivityDetailLoading,
        isActivityDetailRefreshing = isActivityDetailRefreshing,
        csvExportFormat = csvExportFormat,
    )

fun DashboardUiState.toBikeDetailScreenUiState(): BikeDetailScreenUiState =
    BikeDetailScreenUiState(
        selectedBikeDetail = selectedBikeDetail,
        selectedBikeId = selectedBikeId,
        isBikeDetailLoading = isBikeDetailLoading,
        isBikeDetailRefreshing = isBikeDetailRefreshing,
    )
