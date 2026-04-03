package info.meuse24.m24bikestats.presentation.dashboard

data class DashboardUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMoreActivities: Boolean = false,
    val isExportingActivitiesCsv: Boolean = false,
    val activityTotalCount: Int = 0,
    val loadedActivityCount: Int = 0,
    val canLoadMoreActivities: Boolean = false,
    val activities: List<ActivityCardUiModel> = emptyList(),
    val bikes: List<BikeCardUiModel> = emptyList(),
    val pendingActivitiesCsvExport: ActivitiesCsvExportUiModel? = null,
    val selectedActivityDetail: ActivityDetailUiModel? = null,
    val selectedActivityId: String? = null,
    val isActivityDetailLoading: Boolean = false,
    val selectedBikeDetail: BikeDetailUiModel? = null,
    val selectedBikeId: String? = null,
    val isBikeDetailLoading: Boolean = false,
    val error: String? = null,
)

data class ActivityCardUiModel(
    val id: String,
    val title: String,
    val startedAt: String,
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
