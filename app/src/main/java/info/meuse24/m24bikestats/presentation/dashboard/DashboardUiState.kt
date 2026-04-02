package info.meuse24.m24bikestats.presentation.dashboard

data class DashboardUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val activities: List<ActivityCardUiModel> = emptyList(),
    val bikes: List<BikeCardUiModel> = emptyList(),
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
    val title: String,
    val metrics: List<Pair<String, String>>,
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
)
