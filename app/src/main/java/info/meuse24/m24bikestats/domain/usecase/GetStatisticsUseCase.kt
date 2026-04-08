package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.ActivityStatisticsHighlights
import info.meuse24.m24bikestats.domain.model.ActivityStatisticsOverview
import info.meuse24.m24bikestats.domain.model.ActivityStatisticsPeriod
import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.model.StatisticsGrouping
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetStatisticsUseCase(
    private val repository: BoschSmartSystemRepository,
    private val zoneIdProvider: () -> ZoneId = ZoneId::systemDefault,
    private val localeProvider: () -> Locale = Locale::getDefault,
) {
    operator fun invoke(grouping: StatisticsGrouping): Flow<ActivityStatisticsOverview> =
        repository.observeCachedActivities().map { activities ->
            activities.toOverview(
                grouping = grouping,
                zoneId = zoneIdProvider(),
                locale = localeProvider(),
            )
        }

    private fun List<BoschActivity>.toOverview(
        grouping: StatisticsGrouping,
        zoneId: ZoneId,
        locale: Locale,
    ): ActivityStatisticsOverview {
        val totalTours = size
        val totalDistanceKm = sumOf { it.distanceMeters } / 1000.0
        val totalDurationHours = sumOf { it.durationWithoutStopsSeconds }.toDouble() / 3600.0
        val periods = buildPeriods(grouping = grouping, zoneId = zoneId, locale = locale)
        val coveredDates = mapNotNull { it.startTime.toLocalDate(zoneId) }

        return ActivityStatisticsOverview(
            periods = periods,
            coveredPeriodStartEpochMillis = coveredDates.minOrNull()?.atStartOfDay(zoneId)?.toInstant()?.toEpochMilli(),
            coveredPeriodEndEpochMillis = coveredDates.maxOrNull()?.atStartOfDay(zoneId)?.toInstant()?.toEpochMilli(),
            totalTours = totalTours,
            totalDistanceKm = totalDistanceKm,
            totalDurationHours = totalDurationHours,
            avgDistanceKm = if (totalTours > 0) totalDistanceKm / totalTours else 0.0,
            avgDurationHours = if (totalTours > 0) totalDurationHours / totalTours else 0.0,
            highlights = buildHighlights(
                periods = periods,
                zoneId = zoneId,
                locale = locale,
                totalDistanceKm = totalDistanceKm,
                totalDurationHours = totalDurationHours,
            ),
        )
    }

    private fun List<BoschActivity>.buildHighlights(
        periods: List<ActivityStatisticsPeriod>,
        zoneId: ZoneId,
        locale: Locale,
        totalDistanceKm: Double,
        totalDurationHours: Double,
    ): ActivityStatisticsHighlights? {
        if (isEmpty()) return null

        val dayOfWeekDistribution = mapNotNull { it.startTime.toLocalDate(zoneId)?.dayOfWeek }
            .groupingBy { it }
            .eachCount()
        val favoriteDayOfWeek = dayOfWeekDistribution.maxByOrNull(Map.Entry<DayOfWeek, Int>::value)?.key

        val toursPerWeek = mapNotNull { it.startTime.toLocalDate(zoneId)?.toPeriodStart(StatisticsGrouping.WEEK, locale) }
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

        return ActivityStatisticsHighlights(
            longestTourKm = maxOf { it.distanceMeters } / 1000.0,
            longestRideHours = maxOf { it.durationWithoutStopsSeconds } / 3600.0,
            totalElevationGainM = sumOf { it.elevationGainMeters ?: 0 },
            maxSpeedKmh = mapNotNull(BoschActivity::maxSpeedKmh).maxOrNull(),
            fastestTourAvgSpeedKmh = mapNotNull(BoschActivity::averageSpeedKmh).maxOrNull(),
            maxRiderPowerWatts = mapNotNull(BoschActivity::maxRiderPowerWatts).maxOrNull(),
            totalCaloriesBurned = mapNotNull(BoschActivity::caloriesBurned).takeIf { it.isNotEmpty() }?.sum(),
            avgTravelSpeedKmh = if (totalDurationHours > 0.0) totalDistanceKm / totalDurationHours else null,
            mostActivePeriodStartEpochMillis = periods.maxWithOrNull(
                compareBy<ActivityStatisticsPeriod> { it.distanceKm }
                    .thenBy { it.tourCount }
                    .thenBy { it.durationMinutes }
                    .thenBy { it.startEpochMillis },
            )?.startEpochMillis,
            favoriteDayOfWeek = favoriteDayOfWeek,
            dayOfWeekDistribution = dayOfWeekDistribution,
            weeklyFrequencyHistogram = allWeeks
                .map { weekStart -> toursPerWeek[weekStart] ?: 0 }
                .groupingBy { it }
                .eachCount()
                .toSortedMap(),
            activeWeeksRatio = if (allWeeks.size >= 2) {
                toursPerWeek.size.toDouble() / allWeeks.size
            } else {
                null
            },
        )
    }

    private fun List<BoschActivity>.buildPeriods(
        grouping: StatisticsGrouping,
        zoneId: ZoneId,
        locale: Locale,
    ): List<ActivityStatisticsPeriod> {
        val buckets = linkedMapOf<LocalDate, MutableStatisticsPeriod>()

        forEach { activity ->
            val startedAt = activity.startTime.toLocalDate(zoneId) ?: return@forEach
            val periodStart = startedAt.toPeriodStart(grouping, locale)
            val bucket = buckets.getOrPut(periodStart) {
                MutableStatisticsPeriod(periodStart = periodStart)
            }
            bucket.tourCount += 1
            bucket.distanceMeters += activity.distanceMeters
            bucket.durationSeconds += activity.durationWithoutStopsSeconds
        }

        return buckets.values
            .sortedBy { it.periodStart }
            .map { bucket ->
                val periodEnd = bucket.periodStart.toPeriodEnd(grouping)
                ActivityStatisticsPeriod(
                    startEpochMillis = bucket.periodStart.atStartOfDay(zoneId).toInstant().toEpochMilli(),
                    endEpochMillis = periodEnd.atStartOfDay(zoneId).toInstant().toEpochMilli(),
                    tourCount = bucket.tourCount,
                    distanceKm = bucket.distanceMeters / 1000.0,
                    durationMinutes = bucket.durationSeconds / 60,
                )
            }
    }

    private fun String.toLocalDate(zoneId: ZoneId): LocalDate? =
        runCatching {
            Instant.parse(this).atZone(zoneId).toLocalDate()
        }.getOrNull()

    private fun LocalDate.toPeriodStart(
        grouping: StatisticsGrouping,
        locale: Locale,
    ): LocalDate = when (grouping) {
        StatisticsGrouping.WEEK -> with(WeekFields.of(locale).dayOfWeek(), 1L)
        StatisticsGrouping.MONTH -> withDayOfMonth(1)
        StatisticsGrouping.YEAR -> withDayOfYear(1)
    }

    private fun LocalDate.toPeriodEnd(grouping: StatisticsGrouping): LocalDate = when (grouping) {
        StatisticsGrouping.WEEK -> plusDays(6)
        StatisticsGrouping.MONTH -> with(TemporalAdjusters.lastDayOfMonth())
        StatisticsGrouping.YEAR -> with(TemporalAdjusters.lastDayOfYear())
    }

    private data class MutableStatisticsPeriod(
        val periodStart: LocalDate,
        var tourCount: Int = 0,
        var distanceMeters: Int = 0,
        var durationSeconds: Int = 0,
    )
}
