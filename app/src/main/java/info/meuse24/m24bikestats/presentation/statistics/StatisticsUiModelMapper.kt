package info.meuse24.m24bikestats.presentation.statistics

import info.meuse24.m24bikestats.domain.model.BoschActivity
import java.time.Instant
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

class StatisticsUiModelMapper(
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val locale: Locale = Locale.getDefault(),
) {
    fun mapHighlights(
        activities: List<BoschActivity>,
        totalDistanceKm: Double = activities.sumOf { it.distanceMeters } / 1000.0,
        totalDurationHours: Double = activities.sumOf { it.durationWithoutStopsSeconds } / 3600.0,
    ): StatisticsHighlights? {
        if (activities.isEmpty()) return null

        val longestTourKm = activities.maxOf { it.distanceMeters } / 1000.0
        val totalElevationGainM = activities.sumOf { it.elevationGainMeters ?: 0 }
        val maxSpeedKmh = activities.mapNotNull(BoschActivity::maxSpeedKmh).maxOrNull()
        val maxRiderPowerWatts = activities.mapNotNull(BoschActivity::maxRiderPowerWatts).maxOrNull()
        val totalCaloriesValues = activities.mapNotNull(BoschActivity::caloriesBurned)
        val totalCaloriesBurned = totalCaloriesValues.takeIf { it.isNotEmpty() }?.sum()
        val avgTravelSpeedKmh = if (totalDurationHours > 0.0) totalDistanceKm / totalDurationHours else null

        val dayOfWeekDistribution = activities
            .mapNotNull { it.startTime.toLocalDate(zoneId)?.dayOfWeek }
            .groupingBy { it }
            .eachCount()
        val favoriteDayOfWeek = dayOfWeekDistribution.maxByOrNull(Map.Entry<DayOfWeek, Int>::value)?.key

        val toursPerWeek = activities
            .mapNotNull { it.startTime.toLocalDate(zoneId)?.toPeriodStart(StatisticsGrouping.WEEK, locale) }
            .groupingBy { it }
            .eachCount()

        val allWeeks = toursPerWeek.keys.minOrNull()
            ?.let { firstWeek ->
                val lastWeek = toursPerWeek.keys.maxOrNull() ?: firstWeek
                generateSequence(firstWeek) { current ->
                    current.plusWeeks(1).takeUnless { it.isAfter(lastWeek) }
                }.toList()
            }
            .orEmpty()

        val weeklyFrequencyHistogram = allWeeks
            .map { weekStart -> toursPerWeek[weekStart] ?: 0 }
            .groupingBy { it }
            .eachCount()
            .toSortedMap()

        val activeWeeksRatio = if (allWeeks.size >= 2) {
            toursPerWeek.size.toDouble() / allWeeks.size
        } else {
            null
        }

        return StatisticsHighlights(
            longestTourKm = longestTourKm,
            totalElevationGainM = totalElevationGainM,
            maxSpeedKmh = maxSpeedKmh,
            maxRiderPowerWatts = maxRiderPowerWatts,
            totalCaloriesBurned = totalCaloriesBurned,
            avgTravelSpeedKmh = avgTravelSpeedKmh,
            favoriteDayOfWeek = favoriteDayOfWeek,
            dayOfWeekDistribution = dayOfWeekDistribution,
            weeklyFrequencyHistogram = weeklyFrequencyHistogram,
            activeWeeksRatio = activeWeeksRatio,
        )
    }

    fun mapPeriods(
        activities: List<BoschActivity>,
        grouping: StatisticsGrouping,
    ): List<PeriodStats> {
        val bucketBuilder = linkedMapOf<LocalDate, MutablePeriodStats>()

        activities.forEach { activity ->
            val startedAt = activity.startTime.toLocalDate(zoneId) ?: return@forEach
            val periodStart = startedAt.toPeriodStart(grouping, locale)
            val bucket = bucketBuilder.getOrPut(periodStart) {
                MutablePeriodStats(periodStart = periodStart)
            }
            bucket.tourCount += 1
            bucket.distanceMeters += activity.distanceMeters
            bucket.durationSeconds += activity.durationWithoutStopsSeconds
        }

        return bucketBuilder.values
            .sortedBy { it.periodStart }
            .map { bucket ->
                bucket.toUiModel(
                    grouping = grouping,
                    zoneId = zoneId,
                    locale = locale,
                )
            }
    }

    private fun MutablePeriodStats.toUiModel(
        grouping: StatisticsGrouping,
        zoneId: ZoneId,
        locale: Locale,
    ): PeriodStats {
        val periodEnd = periodStart.toPeriodEnd(grouping)
        return PeriodStats(
            label = periodStart.toPeriodLabel(grouping, locale),
            dateRangeLabel = periodStart.toPeriodRangeLabel(periodEnd, locale),
            startEpochMillis = periodStart.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            tourCount = tourCount,
            distanceKm = distanceMeters / 1000.0,
            durationMinutes = durationSeconds / 60,
        )
    }

    private fun String.toLocalDate(zoneId: ZoneId): LocalDate? =
        runCatching {
            Instant.parse(this).atZone(zoneId).toLocalDate()
        }.getOrNull()

    private fun LocalDate.toPeriodStart(
        grouping: StatisticsGrouping,
        locale: Locale,
    ): LocalDate = when (grouping) {
        StatisticsGrouping.WEEK -> {
            val weekFields = WeekFields.of(locale)
            with(weekFields.dayOfWeek(), 1L)
        }

        StatisticsGrouping.MONTH -> withDayOfMonth(1)
    }

    private fun LocalDate.toPeriodEnd(grouping: StatisticsGrouping): LocalDate = when (grouping) {
        StatisticsGrouping.WEEK -> plusDays(6)
        StatisticsGrouping.MONTH -> with(TemporalAdjusters.lastDayOfMonth())
    }

    private fun LocalDate.toPeriodLabel(
        grouping: StatisticsGrouping,
        locale: Locale,
    ): String = when (grouping) {
        StatisticsGrouping.WEEK -> {
            val week = get(WeekFields.of(locale).weekOfWeekBasedYear())
            // Intentionally key off the language so German locales like de_AT and de_CH also use "KW".
            if (locale.language == Locale.GERMAN.language) {
                "KW $week"
            } else {
                "W$week"
            }
        }

        StatisticsGrouping.MONTH -> format(DateTimeFormatter.ofPattern("LLL yy", locale))
    }

    private fun LocalDate.toPeriodRangeLabel(
        endDate: LocalDate,
        locale: Locale,
    ): String {
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale)
        return "${format(formatter)} - ${endDate.format(formatter)}"
    }

    private data class MutablePeriodStats(
        val periodStart: LocalDate,
        var tourCount: Int = 0,
        var distanceMeters: Int = 0,
        var durationSeconds: Int = 0,
    )
}
