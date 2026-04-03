package info.meuse24.m24bikestats.presentation.dashboard

import androidx.annotation.StringRes
import info.meuse24.m24bikestats.R
import info.meuse24.m24bikestats.domain.model.CsvSeparator

data class DashboardUiState(
    val isInitialLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMoreActivities: Boolean = false,
    val isSyncingCloudData: Boolean = false,
    val isExportingActivitiesCsv: Boolean = false,
    val isExportingActivityDetailsCsv: Boolean = false,
    val syncLoadedActivityCount: Int = 0,
    val syncTotalActivityCount: Int = 0,
    val exportLoadedActivityCount: Int = 0,
    val exportTotalActivityCount: Int = 0,
    val exportDetailedLoadedActivityCount: Int = 0,
    val exportDetailedTotalActivityCount: Int = 0,
    val activityTotalCount: Int = 0,
    val loadedActivityCount: Int = 0,
    val visibleActivityCount: Int = 0,
    val canLoadMoreActivities: Boolean = false,
    val activitySearchQuery: String = "",
    val activityDateRangeFilter: ActivityDateRangeFilter = ActivityDateRangeFilter.ALL,
    val activitySortOption: ActivitySortOption = ActivitySortOption.NEWEST_FIRST,
    val allActivities: List<ActivityCardUiModel> = emptyList(),
    val activities: List<ActivityCardUiModel> = emptyList(),
    val bikes: List<BikeCardUiModel> = emptyList(),
    val pendingActivitiesCsvExport: ActivitiesCsvExportUiModel? = null,
    val pendingActivityDetailsCsvExport: ActivityDetailsCsvExportUiModel? = null,
    val lastActivitiesCsvExport: ActivitiesCsvExportSummaryUiModel? = null,
    val lastActivityDetailsCsvExport: ActivityDetailsCsvExportSummaryUiModel? = null,
    val selectedActivityDetail: ActivityDetailUiModel? = null,
    val selectedActivityId: String? = null,
    val isActivityDetailLoading: Boolean = false,
    val isActivityDetailRefreshing: Boolean = false,
    val selectedBikeDetail: BikeDetailUiModel? = null,
    val selectedBikeId: String? = null,
    val isBikeDetailLoading: Boolean = false,
    val isBikeDetailRefreshing: Boolean = false,
    val lastCloudSyncSummary: CloudSyncSummaryUiModel? = null,
    val csvSeparator: CsvSeparator = CsvSeparator.COMMA,
    val error: String? = null,
)

data class HomeUiState(
    val allActivities: List<ActivityCardUiModel>,
    val bikes: List<BikeCardUiModel>,
    val loadedActivityCount: Int,
    val visibleActivityCount: Int,
    val isInitialLoading: Boolean,
    val isRefreshing: Boolean,
    val isSyncingCloudData: Boolean,
    val isExportingActivitiesCsv: Boolean,
    val isExportingActivityDetailsCsv: Boolean,
    val syncLoadedActivityCount: Int,
    val syncTotalActivityCount: Int,
    val lastCloudSyncSummary: CloudSyncSummaryUiModel?,
    val lastActivitiesCsvExport: ActivitiesCsvExportSummaryUiModel?,
    val lastActivityDetailsCsvExport: ActivityDetailsCsvExportSummaryUiModel?,
)

data class ActivitiesUiState(
    val activities: List<ActivityCardUiModel>,
    val activitySearchQuery: String,
    val activityDateRangeFilter: ActivityDateRangeFilter,
    val activitySortOption: ActivitySortOption,
    val visibleActivityCount: Int,
    val loadedActivityCount: Int,
    val activityTotalCount: Int,
    val isRefreshing: Boolean,
    val isLoadingMoreActivities: Boolean,
    val canLoadMoreActivities: Boolean,
)

data class FunctionsUiState(
    val csvSeparator: CsvSeparator,
    val loadedActivityCount: Int,
    val visibleActivityCount: Int,
    val activityTotalCount: Int,
    val isInitialLoading: Boolean,
    val isRefreshing: Boolean,
    val isExportingActivitiesCsv: Boolean,
    val isExportingActivityDetailsCsv: Boolean,
    val exportLoadedActivityCount: Int,
    val exportTotalActivityCount: Int,
    val exportDetailedLoadedActivityCount: Int,
    val exportDetailedTotalActivityCount: Int,
    val pendingActivitiesCsvExport: ActivitiesCsvExportUiModel?,
    val pendingActivityDetailsCsvExport: ActivityDetailsCsvExportUiModel?,
    val lastActivitiesCsvExport: ActivitiesCsvExportSummaryUiModel?,
    val lastActivityDetailsCsvExport: ActivityDetailsCsvExportSummaryUiModel?,
)

data class BikeListUiState(
    val bikes: List<BikeCardUiModel>,
    val isRefreshing: Boolean,
)

data class ActivityDetailScreenUiState(
    val selectedActivityDetail: ActivityDetailUiModel?,
    val selectedActivityId: String?,
    val isActivityDetailLoading: Boolean,
    val isActivityDetailRefreshing: Boolean,
)

data class TrackUiState(
    val selectedActivityDetail: ActivityDetailUiModel?,
    val selectedActivityId: String?,
    val isActivityDetailLoading: Boolean,
    val isActivityDetailRefreshing: Boolean,
    val csvSeparator: CsvSeparator,
)

data class BikeDetailScreenUiState(
    val selectedBikeDetail: BikeDetailUiModel?,
    val selectedBikeId: String?,
    val isBikeDetailLoading: Boolean,
    val isBikeDetailRefreshing: Boolean,
)

data class ActivityCardUiModel(
    val id: String,
    val title: String,
    val startedAt: String,
    val startedAtEpochMillis: Long?,
    val distanceMeters: Int,
    val durationSeconds: Int,
    val dateLabel: String,
    val distanceLabel: String,
    val durationLabel: String,
    val speedLabel: String,
    val powerLabel: String?,
    val elevationLabel: String?,
    val caloriesLabel: String?,
)

data class ActivityDetailUiModel(
    val id: String,
    val title: String,
    val subtitle: String?,
    val summary: List<Pair<String, String>>,
    val sections: List<DetailSectionUiModel>,
    val trackPoints: List<ActivityTrackPointUiModel>,
    val profilePoints: List<ActivityProfilePointUiModel>,
)

data class BikeCardUiModel(
    val id: String,
    val title: String,
    val subtitle: String?,
    val odometerLabel: String?,
    val assistSpeedLabel: String?,
    val batterySummary: String?,
)

data class BikeDetailUiModel(
    val title: String,
    val subtitle: String?,
    val sections: List<DetailSectionUiModel>,
)

data class DetailSectionUiModel(
    val title: String,
    val rows: List<Pair<String, String>>,
    val actions: List<DetailSectionActionUiModel> = emptyList(),
)

data class DetailSectionActionUiModel(
    val label: String,
    val type: DetailSectionActionType,
)

enum class DetailSectionActionType {
    SHARE,
    MAP,
}

data class ActivityTrackPointUiModel(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double?,
    val distanceMeters: Double?,
)

data class ActivityProfilePointUiModel(
    val distanceMeters: Double,
    val altitudeMeters: Double?,
    val speedKmh: Double?,
    val cadenceRpm: Double?,
    val riderPowerWatts: Double?,
)

data class ActivitiesCsvExportUiModel(
    val fileName: String,
    val csvContent: String,
    val activityCount: Int,
)

data class ActivitiesCsvExportSummaryUiModel(
    val fileName: String,
    val activityCount: Int,
    val exportedAtLabel: String,
)

data class ActivityDetailsCsvExportUiModel(
    val fileName: String,
    val csvContent: String,
    val activityCount: Int,
    val detailPointCount: Int,
)

data class ActivityDetailsCsvExportSummaryUiModel(
    val fileName: String,
    val activityCount: Int,
    val detailPointCount: Int,
    val exportedAtLabel: String,
)

data class CloudSyncSummaryUiModel(
    val activityCount: Int,
    val bikeCount: Int,
    val syncedAtLabel: String,
)

enum class ActivityDateRangeFilter(@param:StringRes val labelRes: Int) {
    ALL(R.string.date_filter_all),
    LAST_30_DAYS(R.string.date_filter_last_30_days),
    LAST_12_MONTHS(R.string.date_filter_last_12_months),
}

enum class ActivitySortOption(@param:StringRes val labelRes: Int) {
    NEWEST_FIRST(R.string.sort_newest),
    OLDEST_FIRST(R.string.sort_oldest),
    LONGEST_DISTANCE(R.string.sort_distance),
    LONGEST_DURATION(R.string.sort_duration),
}

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
        syncLoadedActivityCount = syncLoadedActivityCount,
        syncTotalActivityCount = syncTotalActivityCount,
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
        csvSeparator = csvSeparator,
        loadedActivityCount = loadedActivityCount,
        visibleActivityCount = visibleActivityCount,
        activityTotalCount = activityTotalCount,
        isInitialLoading = isInitialLoading,
        isRefreshing = isRefreshing,
        isExportingActivitiesCsv = isExportingActivitiesCsv,
        isExportingActivityDetailsCsv = isExportingActivityDetailsCsv,
        exportLoadedActivityCount = exportLoadedActivityCount,
        exportTotalActivityCount = exportTotalActivityCount,
        exportDetailedLoadedActivityCount = exportDetailedLoadedActivityCount,
        exportDetailedTotalActivityCount = exportDetailedTotalActivityCount,
        pendingActivitiesCsvExport = pendingActivitiesCsvExport,
        pendingActivityDetailsCsvExport = pendingActivityDetailsCsvExport,
        lastActivitiesCsvExport = lastActivitiesCsvExport,
        lastActivityDetailsCsvExport = lastActivityDetailsCsvExport,
    )

fun DashboardUiState.toBikeListUiState(): BikeListUiState =
    BikeListUiState(
        bikes = bikes,
        isRefreshing = isRefreshing,
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
        csvSeparator = csvSeparator,
    )

fun DashboardUiState.toBikeDetailScreenUiState(): BikeDetailScreenUiState =
    BikeDetailScreenUiState(
        selectedBikeDetail = selectedBikeDetail,
        selectedBikeId = selectedBikeId,
        isBikeDetailLoading = isBikeDetailLoading,
        isBikeDetailRefreshing = isBikeDetailRefreshing,
    )
