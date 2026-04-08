package info.meuse24.m24bikestats.presentation.dashboard
import androidx.annotation.StringRes
import info.meuse24.m24bikestats.R

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
    val overview: ActivityCardUiModel,
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
    val walkAssistLabel: String?,
    val powerOnSummary: String?,
    val assistModesSummary: String?,
    val batterySummary: String?,
    val bikePassSummary: String?,
    val shareText: String,
)

data class BikeDetailUiModel(
    val title: String,
    val subtitle: String?,
    val sections: List<DetailSectionUiModel>,
)

data class DetailSectionUiModel(
    val title: String,
    val rows: List<Pair<String, String>>,
    val indicator: DetailSectionIndicatorUiModel? = null,
    val actions: List<DetailSectionActionUiModel> = emptyList(),
)

data class DetailSectionIndicatorUiModel(
    val label: String,
    val value: String,
    val progress: Float,
    val tone: DetailSectionIndicatorTone,
    val supportingText: String? = null,
)

enum class DetailSectionIndicatorTone {
    POSITIVE,
    INFORMATIVE,
    WARNING,
    DANGER,
}

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

data class PdfExportUiModel(
    val fileName: String,
    val filePath: String,
)

data class PdfExportSummaryUiModel(
    val fileName: String,
    val exportedAtLabel: String,
)

data class CloudSyncSummaryUiModel(
    val activityCount: Int,
    val bikeCount: Int,
    val syncedAtLabel: String,
)

data class DataStatusUiModel(
    val statusTone: DataStatusTone,
    val statusLabel: String,
    val statusSummary: String,
    val coveredPeriodLabel: String?,
    val cachedActivityCount: Int,
    val detailedActivityCount: Int,
    val detailCoverageLabel: String,
    val missingDetailCount: Int,
    val staleDetailCount: Int,
    val gpsPointCount: Int,
    val lastActivitySyncLabel: String?,
    val lastBikeSyncLabel: String?,
    val lastDetailSyncLabel: String?,
)

enum class DataStatusTone {
    EMPTY,
    PARTIAL,
    STALE,
    COMPLETE,
}

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
