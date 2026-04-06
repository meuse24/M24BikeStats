package info.meuse24.m24bikestats.presentation.statistics

import info.meuse24.m24bikestats.domain.model.BoschActivity
import java.time.Instant
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
