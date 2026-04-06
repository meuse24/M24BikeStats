package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.model.PdfReportActivitySummary
import info.meuse24.m24bikestats.domain.model.PdfReportData
import info.meuse24.m24bikestats.domain.model.PdfReportHighlights
import info.meuse24.m24bikestats.domain.model.PdfReportPeriod
import info.meuse24.m24bikestats.domain.model.PdfReportStatistics
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository
import info.meuse24.m24bikestats.domain.repository.PdfReportMetadataRepository
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.flow.first

class ExportPdfSummaryReportUseCase(
    private val metadataRepository: PdfReportMetadataRepository,
    private val repository: BoschSmartSystemRepository,
    private val localeProvider: () -> Locale = Locale::getDefault,
    private val zoneIdProvider: () -> ZoneId = ZoneId::systemDefault,
    private val clock: () -> Instant = Instant::now,
) {
    suspend operator fun invoke(): Result<PdfReportData> = runCatching {
        val locale = localeProvider()
        val zoneId = zoneIdProvider()
        val userInfo = metadataRepository.getCurrentUserInfo().getOrThrow()
        val discoveryInfo = metadataRepository.getCurrentDiscoveryInfo().getOrThrow()
        val bikes = repository.observeCachedBikes().first()
        val activities = repository.getCachedActivities()

        PdfReportData(
            generatedAt = clock(),
            userInfo = userInfo,
            discoveryInfo = discoveryInfo,
            bikes = bikes,
            activitySummary = activities.toActivitySummary(),
            statistics = activities.toStatistics(locale, zoneId),
        )
    }

    private fun List<BoschActivity>.toActivitySummary(): PdfReportActivitySummary {
        val totalTours = size
        val totalDistanceKm = sumOf { it.distanceMeters } / 1000.0
        val totalDurationHours = sumOf { it.durationWithoutStopsSeconds }.toDouble() / 3600.0
        val startInstants = mapNotNull { it.startTime.toInstantOrNull() }
        val calories = mapNotNull(BoschActivity::caloriesBurned)

        return PdfReportActivitySummary(
            totalTours = totalTours,
            totalDistanceKm = totalDistanceKm,
            totalDurationHours = totalDurationHours,
            avgDistanceKm = if (totalTours > 0) totalDistanceKm / totalTours else 0.0,
            avgDurationHours = if (totalTours > 0) totalDurationHours / totalTours else 0.0,
            earliestActivityDate = startInstants.minOrNull(),
            latestActivityDate = startInstants.maxOrNull(),
            avgTravelSpeedKmh = if (totalDurationHours > 0.0) totalDistanceKm / totalDurationHours else null,
            totalElevationGainM = sumOf { it.elevationGainMeters ?: 0 },
            totalCaloriesBurned = calories.takeIf { it.isNotEmpty() }?.sum(),
        )
    }

    private fun List<BoschActivity>.toStatistics(
        locale: Locale,
        zoneId: ZoneId,
    ): PdfReportStatistics {
        val monthlyPeriods = buildMonthlyPeriods(locale, zoneId)
        val dayOfWeekDistribution = mapNotNull { it.startTime.toLocalDate(zoneId)?.dayOfWeek }
            .groupingBy { it }
            .eachCount()
        val favoriteDayOfWeek = dayOfWeekDistribution.maxByOrNull(Map.Entry<DayOfWeek, Int>::value)?.key
        val toursPerWeek = mapNotNull { it.startTime.toLocalDate(zoneId)?.toWeekStart(locale) }
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

        return PdfReportStatistics(
            monthlyPeriods = monthlyPeriods,
            highlights = PdfReportHighlights(
                longestTourKm = maxOfOrNull { it.distanceMeters }?.div(1000.0) ?: 0.0,
                maxSpeedKmh = mapNotNull(BoschActivity::maxSpeedKmh).maxOrNull(),
                maxRiderPowerWatts = mapNotNull(BoschActivity::maxRiderPowerWatts).maxOrNull(),
                favoriteDayOfWeek = favoriteDayOfWeek,
            ),
            dayOfWeekDistribution = dayOfWeekDistribution,
            weeklyFrequencyHistogram = allWeeks
                .map { weekStart -> toursPerWeek[weekStart] ?: 0 }
                .groupingBy { it }
                .eachCount()
                .toSortedMap(),
            activeWeeksRatio = if (allWeeks.size >= 2) toursPerWeek.size.toDouble() / allWeeks.size else null,
        )
    }

    private fun List<BoschActivity>.buildMonthlyPeriods(
        locale: Locale,
        zoneId: ZoneId,
    ): List<PdfReportPeriod> {
        val buckets = linkedMapOf<LocalDate, MutablePdfReportPeriod>()

        forEach { activity ->
            val localDate = activity.startTime.toLocalDate(zoneId) ?: return@forEach
            val monthStart = localDate.withDayOfMonth(1)
            val bucket = buckets.getOrPut(monthStart) { MutablePdfReportPeriod(periodStart = monthStart) }
            bucket.tourCount += 1
            bucket.distanceMeters += activity.distanceMeters
            bucket.durationSeconds += activity.durationWithoutStopsSeconds
        }

        return buckets.values
            .sortedBy { it.periodStart }
            .map { bucket ->
                PdfReportPeriod(
                    label = bucket.periodStart.format(DateTimeFormatter.ofPattern("LLL yy", locale)),
                    tourCount = bucket.tourCount,
                    distanceKm = bucket.distanceMeters / 1000.0,
                    durationHours = bucket.durationSeconds / 3600.0,
                )
            }
    }

    private fun String.toInstantOrNull(): Instant? =
        runCatching { Instant.parse(this) }.getOrNull()

    private fun String.toLocalDate(zoneId: ZoneId): LocalDate? =
        toInstantOrNull()?.atZone(zoneId)?.toLocalDate()

    private fun LocalDate.toWeekStart(locale: Locale): LocalDate =
        with(WeekFields.of(locale).dayOfWeek(), 1L)

    private data class MutablePdfReportPeriod(
        val periodStart: LocalDate,
        var tourCount: Int = 0,
        var distanceMeters: Int = 0,
        var durationSeconds: Int = 0,
    )
}
