package info.meuse24.m24bikestats.presentation.statistics

data class StatisticsUiState(
    val periods: List<PeriodStats> = emptyList(),
    val selectedPeriod: PeriodStats? = null,
    val grouping: StatisticsGrouping = StatisticsGrouping.MONTH,
    val totalTours: Int = 0,
    val totalDistanceKm: Double = 0.0,
    val totalDurationHours: Double = 0.0,
    val isLoading: Boolean = false,
)

data class PeriodStats(
    val label: String,
    val dateRangeLabel: String,
    val startEpochMillis: Long,
    val tourCount: Int,
    val distanceKm: Double,
    val durationMinutes: Int,
)

enum class StatisticsGrouping {
    WEEK,
    MONTH,
}
