package info.meuse24.m24bikestats.presentation.statistics

import java.util.Locale

data class StatisticsUiState(
    val periods: List<PeriodStats> = emptyList(),
    val selectedPeriod: PeriodStats? = null,
    val grouping: StatisticsGrouping = StatisticsGrouping.MONTH,
    val totalTours: Int = 0,
    val totalDistanceKm: Double = 0.0,
    val totalDurationHours: Double = 0.0,
    val avgDistanceKm: Double = 0.0,
    val avgDurationHours: Double = 0.0,
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

internal val PeriodStats.durationHours: Double
    get() = durationMinutes / 60.0

internal fun Double.toReadableDistance(locale: Locale = Locale.getDefault()): String =
    String.format(locale, "%.1f km", this)

internal fun Double.toReadableHours(locale: Locale = Locale.getDefault()): String =
    String.format(locale, "%.1f h", this)
