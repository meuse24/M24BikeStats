package info.meuse24.m24bikestats.presentation.statistics

import java.time.DayOfWeek
import java.util.Locale
import kotlin.math.roundToInt

data class StatisticsUiState(
    val periods: List<PeriodStats> = emptyList(),
    val selectedPeriod: PeriodStats? = null,
    val grouping: StatisticsGrouping = StatisticsGrouping.MONTH,
    val totalTours: Int = 0,
    val totalDistanceKm: Double = 0.0,
    val totalDurationHours: Double = 0.0,
    val avgDistanceKm: Double = 0.0,
    val avgDurationHours: Double = 0.0,
    val highlights: StatisticsHighlights? = null,
    val isLoading: Boolean = false,
)

data class StatisticsHighlights(
    val longestTourKm: Double,
    val longestRideHours: Double,
    val totalElevationGainM: Int,
    val maxSpeedKmh: Double?,
    val fastestTourAvgSpeedKmh: Double?,
    val maxRiderPowerWatts: Double?,
    val totalCaloriesBurned: Double?,
    val avgTravelSpeedKmh: Double?,
    val mostActivePeriod: StatisticsActivePeriod?,
    val favoriteDayOfWeek: DayOfWeek?,
    val dayOfWeekDistribution: Map<DayOfWeek, Int>,
    val weeklyFrequencyHistogram: Map<Int, Int>,
    val activeWeeksRatio: Double?,
)

data class StatisticsActivePeriod(
    val label: String,
    val dateRangeLabel: String,
    val distanceKm: Double,
    val tourCount: Int,
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
    YEAR,
}

internal val PeriodStats.durationHours: Double
    get() = durationMinutes / 60.0

internal fun Double.toReadableDistance(locale: Locale = Locale.getDefault()): String =
    String.format(locale, "%.1f km", this)

internal fun Double.toReadableHours(locale: Locale = Locale.getDefault()): String =
    String.format(locale, "%.1f h", this)

internal fun Double.toReadableSpeed(locale: Locale = Locale.getDefault()): String =
    String.format(locale, "%.1f km/h", this)

internal fun Int.toReadableMeters(locale: Locale = Locale.getDefault()): String =
    String.format(locale, "%,d m", this)

internal fun Double.toReadableWatts(locale: Locale = Locale.getDefault()): String =
    String.format(locale, "%d W", roundToInt())

internal fun Double.toReadableCalories(locale: Locale = Locale.getDefault()): String =
    String.format(locale, "%,.0f kcal", this)
