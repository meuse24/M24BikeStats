package info.meuse24.m24bikestats.domain.model

import java.time.DayOfWeek

data class ActivityStatisticsOverview(
    val periods: List<ActivityStatisticsPeriod> = emptyList(),
    val coveredPeriodStartEpochMillis: Long? = null,
    val coveredPeriodEndEpochMillis: Long? = null,
    val totalTours: Int = 0,
    val totalDistanceKm: Double = 0.0,
    val totalDurationHours: Double = 0.0,
    val avgDistanceKm: Double = 0.0,
    val avgDurationHours: Double = 0.0,
    val highlights: ActivityStatisticsHighlights? = null,
)

data class ActivityStatisticsPeriod(
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val tourCount: Int,
    val distanceKm: Double,
    val durationMinutes: Int,
)

data class ActivityStatisticsHighlights(
    val longestTourKm: Double,
    val longestRideHours: Double,
    val totalElevationGainM: Int,
    val maxSpeedKmh: Double?,
    val fastestTourAvgSpeedKmh: Double?,
    val maxRiderPowerWatts: Double?,
    val totalCaloriesBurned: Double?,
    val avgTravelSpeedKmh: Double?,
    val mostActivePeriodStartEpochMillis: Long?,
    val favoriteDayOfWeek: DayOfWeek?,
    val dayOfWeekDistribution: Map<DayOfWeek, Int>,
    val weeklyFrequencyHistogram: Map<Int, Int>,
    val activeWeeksRatio: Double?,
)

enum class StatisticsGrouping {
    WEEK,
    MONTH,
    YEAR,
}
