package info.meuse24.m24bikestats.presentation.dashboard

import info.meuse24.m24bikestats.domain.model.CsvSeparator

data class DashboardUiState(
    val isInitialLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMoreActivities: Boolean = false,
    val isExportingActivitiesCsv: Boolean = false,
    val isExportingActivityDetailsCsv: Boolean = false,
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
    val csvSeparator: CsvSeparator = CsvSeparator.COMMA,
    val error: String? = null,
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

enum class ActivityDateRangeFilter(val label: String) {
    ALL("Alle"),
    LAST_30_DAYS("30 Tage"),
    LAST_12_MONTHS("12 Monate"),
}

enum class ActivitySortOption(val label: String) {
    NEWEST_FIRST("Neueste"),
    OLDEST_FIRST("Älteste"),
    LONGEST_DISTANCE("Distanz"),
    LONGEST_DURATION("Dauer"),
}
