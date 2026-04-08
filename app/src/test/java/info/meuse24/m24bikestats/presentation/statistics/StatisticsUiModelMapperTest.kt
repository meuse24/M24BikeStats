package info.meuse24.m24bikestats.presentation.statistics

import info.meuse24.m24bikestats.domain.model.ActivityStatisticsHighlights
import info.meuse24.m24bikestats.domain.model.ActivityStatisticsOverview
import info.meuse24.m24bikestats.domain.model.ActivityStatisticsPeriod
import info.meuse24.m24bikestats.domain.model.StatisticsGrouping
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class StatisticsUiModelMapperTest {

    private val zoneId = ZoneId.of("Europe/Vienna")
    private val mapper = StatisticsUiModelMapper(
        zoneId = zoneId,
        locale = Locale.GERMAN,
    )

    @Test
    fun `maps monthly period into localized labels`() {
        val period = ActivityStatisticsPeriod(
            startEpochMillis = LocalDate.of(2026, 4, 1).atStartOfDay(zoneId).toInstant().toEpochMilli(),
            endEpochMillis = LocalDate.of(2026, 4, 30).atStartOfDay(zoneId).toInstant().toEpochMilli(),
            tourCount = 2,
            distanceKm = 15.0,
            durationMinutes = 45,
        )

        val mapped = mapper.toPeriodStats(period, StatisticsGrouping.MONTH)

        assertEquals("Apr 26", mapped.label)
        assertEquals("01.04.26 - 30.04.26", mapped.dateRangeLabel)
        assertEquals(2, mapped.tourCount)
        assertEquals(15.0, mapped.distanceKm, 0.0)
    }

    @Test
    fun `maps overview totals and selected period into ui state`() {
        val april = period(2026, 4, 1, 2026, 4, 30, distanceKm = 40.0, tours = 3, durationMinutes = 120)
        val may = period(2026, 5, 1, 2026, 5, 31, distanceKm = 25.0, tours = 2, durationMinutes = 80)
        val uiState = mapper.toUiState(
            overview = ActivityStatisticsOverview(
                periods = listOf(april, may),
                coveredPeriodStartEpochMillis = LocalDate.of(2026, 4, 3).atStartOfDay(zoneId).toInstant().toEpochMilli(),
                coveredPeriodEndEpochMillis = LocalDate.of(2026, 5, 18).atStartOfDay(zoneId).toInstant().toEpochMilli(),
                totalTours = 5,
                totalDistanceKm = 65.0,
                totalDurationHours = 200.0 / 60.0,
                avgDistanceKm = 13.0,
                avgDurationHours = (200.0 / 60.0) / 5,
                highlights = null,
            ),
            grouping = StatisticsGrouping.MONTH,
            selectedPeriodStart = may.startEpochMillis,
        )

        assertEquals(5, uiState.totalTours)
        assertEquals(65.0, uiState.totalDistanceKm, 0.0)
        assertEquals("Mai 26", uiState.selectedPeriod?.label)
        assertEquals(may.startEpochMillis, uiState.selectedPeriod?.startEpochMillis)
        assertEquals("03.04.26 - 18.05.26", uiState.coveredPeriodLabel)
    }

    @Test
    fun `maps highlight reference to active period ui model`() {
        val period = period(2026, 4, 1, 2026, 4, 30, distanceKm = 42.0, tours = 2, durationMinutes = 90)
        val uiState = mapper.toUiState(
            overview = ActivityStatisticsOverview(
                periods = listOf(period),
                totalTours = 2,
                totalDistanceKm = 42.0,
                totalDurationHours = 1.5,
                avgDistanceKm = 21.0,
                avgDurationHours = 0.75,
                highlights = ActivityStatisticsHighlights(
                    longestTourKm = 30.0,
                    longestRideHours = 1.2,
                    totalElevationGainM = 400,
                    maxSpeedKmh = 42.0,
                    fastestTourAvgSpeedKmh = 24.0,
                    maxRiderPowerWatts = 380.0,
                    totalCaloriesBurned = 900.0,
                    avgTravelSpeedKmh = 28.0,
                    mostActivePeriodStartEpochMillis = period.startEpochMillis,
                    favoriteDayOfWeek = DayOfWeek.SATURDAY,
                    dayOfWeekDistribution = mapOf(DayOfWeek.SATURDAY to 2),
                    weeklyFrequencyHistogram = mapOf(1 to 2),
                    activeWeeksRatio = 1.0,
                ),
            ),
            grouping = StatisticsGrouping.MONTH,
            selectedPeriodStart = null,
        )

        val highlights = uiState.highlights
        assertNotNull(highlights)
        assertEquals("Apr 26", highlights?.mostActivePeriod?.label)
        assertEquals(42.0, highlights?.mostActivePeriod?.distanceKm ?: 0.0, 0.0)
        assertEquals(DayOfWeek.SATURDAY, highlights?.favoriteDayOfWeek)
    }

    private fun period(
        startYear: Int,
        startMonth: Int,
        startDay: Int,
        endYear: Int,
        endMonth: Int,
        endDay: Int,
        distanceKm: Double,
        tours: Int,
        durationMinutes: Int,
    ): ActivityStatisticsPeriod =
        ActivityStatisticsPeriod(
            startEpochMillis = LocalDate.of(startYear, startMonth, startDay).atStartOfDay(zoneId).toInstant().toEpochMilli(),
            endEpochMillis = LocalDate.of(endYear, endMonth, endDay).atStartOfDay(zoneId).toInstant().toEpochMilli(),
            tourCount = tours,
            distanceKm = distanceKm,
            durationMinutes = durationMinutes,
        )
}
