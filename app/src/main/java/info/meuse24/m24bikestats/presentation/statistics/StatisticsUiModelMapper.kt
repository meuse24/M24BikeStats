package info.meuse24.m24bikestats.presentation.statistics

import info.meuse24.m24bikestats.domain.model.ActivityStatisticsHighlights
import info.meuse24.m24bikestats.domain.model.ActivityStatisticsOverview
import info.meuse24.m24bikestats.domain.model.ActivityStatisticsPeriod
import info.meuse24.m24bikestats.domain.model.StatisticsGrouping
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.WeekFields
import java.util.Locale

class StatisticsUiModelMapper(
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val locale: Locale = Locale.getDefault(),
) {
    fun toUiState(
        overview: ActivityStatisticsOverview,
        grouping: StatisticsGrouping,
        selectedPeriodStart: Long?,
    ): StatisticsUiState {
        val periods = overview.periods.map { period ->
            val startDate = period.startEpochMillis.toLocalDate(zoneId)
            val endDate = period.endEpochMillis.toLocalDate(zoneId)
            PeriodStats(
                label = startDate.toPeriodLabel(grouping, locale),
                dateRangeLabel = startDate.toPeriodRangeLabel(endDate, locale),
                startEpochMillis = period.startEpochMillis,
                tourCount = period.tourCount,
                distanceKm = period.distanceKm,
                durationMinutes = period.durationMinutes,
            )
        }
        val selectedPeriod = periods.firstOrNull { it.startEpochMillis == selectedPeriodStart }

        return StatisticsUiState(
            periods = periods,
            selectedPeriod = selectedPeriod,
            grouping = grouping,
            coveredPeriodLabel = overview.coveredPeriodStartEpochMillis?.let { startEpochMillis ->
                overview.coveredPeriodEndEpochMillis?.let { endEpochMillis ->
                    startEpochMillis.toCoveredPeriodLabel(endEpochMillis, zoneId, locale)
                }
            },
            totalTours = overview.totalTours,
            totalDistanceKm = overview.totalDistanceKm,
            totalDurationHours = overview.totalDurationHours,
            avgDistanceKm = overview.avgDistanceKm,
            avgDurationHours = overview.avgDurationHours,
            highlights = overview.highlights?.toUiModel(periods),
            isLoading = false,
        )
    }

    private fun ActivityStatisticsHighlights.toUiModel(
        periods: List<PeriodStats>,
    ): StatisticsHighlights =
        StatisticsHighlights(
            longestTourKm = longestTourKm,
            longestRideHours = longestRideHours,
            totalElevationGainM = totalElevationGainM,
            maxSpeedKmh = maxSpeedKmh,
            fastestTourAvgSpeedKmh = fastestTourAvgSpeedKmh,
            maxRiderPowerWatts = maxRiderPowerWatts,
            totalCaloriesBurned = totalCaloriesBurned,
            avgTravelSpeedKmh = avgTravelSpeedKmh,
            mostActivePeriod = periods
                .firstOrNull { it.startEpochMillis == mostActivePeriodStartEpochMillis }
                ?.toActivePeriod(),
            favoriteDayOfWeek = favoriteDayOfWeek,
            dayOfWeekDistribution = dayOfWeekDistribution,
            weeklyFrequencyHistogram = weeklyFrequencyHistogram,
            activeWeeksRatio = activeWeeksRatio,
        )

    private fun PeriodStats.toActivePeriod(): StatisticsActivePeriod =
        StatisticsActivePeriod(
            label = label,
            dateRangeLabel = dateRangeLabel,
            distanceKm = distanceKm,
            tourCount = tourCount,
        )

    internal fun toPeriodStats(
        period: ActivityStatisticsPeriod,
        grouping: StatisticsGrouping,
    ): PeriodStats {
        val startDate = period.startEpochMillis.toLocalDate(zoneId)
        val endDate = period.endEpochMillis.toLocalDate(zoneId)
        return PeriodStats(
            label = startDate.toPeriodLabel(grouping, locale),
            dateRangeLabel = startDate.toPeriodRangeLabel(endDate, locale),
            startEpochMillis = period.startEpochMillis,
            tourCount = period.tourCount,
            distanceKm = period.distanceKm,
            durationMinutes = period.durationMinutes,
        )
    }

    private fun Long.toLocalDate(zoneId: ZoneId): LocalDate =
        Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()

    private fun Long.toCoveredPeriodLabel(
        endEpochMillis: Long,
        zoneId: ZoneId,
        locale: Locale,
    ): String {
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale)
        val startDate = toLocalDate(zoneId)
        val endDate = endEpochMillis.toLocalDate(zoneId)
        return if (startDate == endDate) {
            startDate.format(formatter)
        } else {
            "${startDate.format(formatter)} - ${endDate.format(formatter)}"
        }
    }

    private fun LocalDate.toPeriodLabel(
        grouping: StatisticsGrouping,
        locale: Locale,
    ): String = when (grouping) {
        StatisticsGrouping.WEEK -> {
            val week = get(WeekFields.of(locale).weekOfWeekBasedYear())
            if (locale.language == Locale.GERMAN.language) {
                "KW $week"
            } else {
                "W$week"
            }
        }

        StatisticsGrouping.MONTH -> format(DateTimeFormatter.ofPattern("LLL yy", locale))
        StatisticsGrouping.YEAR -> format(DateTimeFormatter.ofPattern("yyyy"))
    }

    private fun LocalDate.toPeriodRangeLabel(
        endDate: LocalDate,
        locale: Locale,
    ): String {
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale)
        return "${format(formatter)} - ${endDate.format(formatter)}"
    }
}
