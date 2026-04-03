package info.meuse24.m24bikestats.presentation.dashboard

import info.meuse24.m24bikestats.domain.model.CsvExportFormat
import info.meuse24.m24bikestats.domain.model.SmartSystemCloudSyncPhase

data class DashboardUiState(
    val isInitialLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMoreActivities: Boolean = false,
    val isSyncingCloudData: Boolean = false,
    val isExportingActivitiesCsv: Boolean = false,
    val isExportingActivityDetailsCsv: Boolean = false,
    val syncPhase: SmartSystemCloudSyncPhase? = null,
    val syncPhaseLabel: String? = null,
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
    val csvExportFormat: CsvExportFormat = CsvExportFormat.SYSTEM_DEFAULT,
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
    val syncPhase: SmartSystemCloudSyncPhase?,
    val syncPhaseLabel: String?,
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
    val csvExportFormat: CsvExportFormat,
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
    val csvExportFormat: CsvExportFormat,
)

data class BikeDetailScreenUiState(
    val selectedBikeDetail: BikeDetailUiModel?,
    val selectedBikeId: String?,
    val isBikeDetailLoading: Boolean,
    val isBikeDetailRefreshing: Boolean,
)
