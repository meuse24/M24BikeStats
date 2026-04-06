package info.meuse24.m24bikestats.presentation.statistics

import info.meuse24.m24bikestats.domain.model.BoschActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneId
import java.util.Locale

class StatisticsUiModelMapperTest {

    private val mapper = StatisticsUiModelMapper(
        zoneId = ZoneId.of("Europe/Vienna"),
        locale = Locale.GERMAN,
    )

    @Test
    fun `groups activities by week and sums metrics`() {
        val result = mapper.mapPeriods(
            activities = listOf(
                activity("2026-04-01T08:00:00Z", distanceMeters = 10000, durationSeconds = 1800),
                activity("2026-04-03T09:00:00Z", distanceMeters = 25000, durationSeconds = 3600),
                activity("2026-04-10T09:00:00Z", distanceMeters = 5000, durationSeconds = 900),
            ),
            grouping = StatisticsGrouping.WEEK,
        )

        assertEquals(2, result.size)
        assertEquals("KW 14", result[0].label)
        assertEquals(2, result[0].tourCount)
        assertEquals(35.0, result[0].distanceKm, 0.0)
        assertEquals(90, result[0].durationMinutes)
        assertEquals(1, result[1].tourCount)
    }

    @Test
    fun `groups activities by month using local timezone`() {
        val result = mapper.mapPeriods(
            activities = listOf(
                activity("2026-03-31T22:30:00Z", distanceMeters = 10000, durationSeconds = 1800),
                activity("2026-04-15T09:00:00Z", distanceMeters = 5000, durationSeconds = 900),
            ),
            grouping = StatisticsGrouping.MONTH,
        )

        assertEquals(1, result.size)
        assertEquals("Apr 26", result[0].label)
        assertEquals(2, result[0].tourCount)
        assertEquals(15.0, result[0].distanceKm, 0.0)
    }

    @Test
    fun `uses localized date range labels`() {
        val usMapper = StatisticsUiModelMapper(
            zoneId = ZoneId.of("UTC"),
            locale = Locale.US,
        )

        val result = usMapper.mapPeriods(
            activities = listOf(
                activity("2026-04-15T09:00:00Z", distanceMeters = 5000, durationSeconds = 900),
            ),
            grouping = StatisticsGrouping.MONTH,
        )

        assertEquals("4/1/26 - 4/30/26", result[0].dateRangeLabel)
    }

    @Test
    fun `map highlights returns null for empty list`() {
        assertNull(mapper.mapHighlights(emptyList()))
    }

    @Test
    fun `map highlights keeps nullable metrics null when source values are missing`() {
        val highlights = mapper.mapHighlights(
            listOf(
                activity("2026-04-01T08:00:00Z", distanceMeters = 10000, durationSeconds = 1800),
                activity("2026-04-08T08:00:00Z", distanceMeters = 5000, durationSeconds = 900),
            ),
        )

        requireNotNull(highlights)
        assertNull(highlights.maxSpeedKmh)
        assertNull(highlights.maxRiderPowerWatts)
        assertNull(highlights.totalCaloriesBurned)
    }

    @Test
    fun `map highlights for single activity keeps active week ratio null and computes favorite day`() {
        val highlights = mapper.mapHighlights(
            listOf(
                activity(
                    startTime = "2026-04-04T08:00:00Z",
                    distanceMeters = 42000,
                    durationSeconds = 7200,
                    elevationGainMeters = 350,
                ),
            ),
        )

        requireNotNull(highlights)
        assertEquals(42.0, highlights.longestTourKm, 0.0)
        assertEquals(350, highlights.totalElevationGainM)
        assertEquals(DayOfWeek.SATURDAY, highlights.favoriteDayOfWeek)
        assertNull(highlights.activeWeeksRatio)
    }

    @Test
    fun `map highlights computes favorite day and active week ratio across multiple weeks`() {
        val highlights = mapper.mapHighlights(
            listOf(
                activity("2026-04-03T08:00:00Z", distanceMeters = 20000, durationSeconds = 3600),
                activity("2026-04-10T09:00:00Z", distanceMeters = 10000, durationSeconds = 1800),
            ),
        )

        requireNotNull(highlights)
        assertEquals(DayOfWeek.FRIDAY, highlights.favoriteDayOfWeek)
        assertEquals(1.0, highlights.activeWeeksRatio!!, 0.0)
        assertEquals(mapOf(1 to 2), highlights.weeklyFrequencyHistogram)
    }

    @Test
    fun `map highlights fills gap weeks in weekly histogram`() {
        val highlights = mapper.mapHighlights(
            listOf(
                activity("2026-04-01T08:00:00Z", distanceMeters = 12000, durationSeconds = 1800),
                activity("2026-04-14T08:00:00Z", distanceMeters = 13000, durationSeconds = 2100),
            ),
        )

        requireNotNull(highlights)
        assertEquals(mapOf(0 to 1, 1 to 2), highlights.weeklyFrequencyHistogram)
        assertEquals(2.0 / 3.0, highlights.activeWeeksRatio!!, 0.0)
    }

    @Test
    fun `map highlights favors dominant weekday distribution`() {
        val highlights = mapper.mapHighlights(
            listOf(
                activity("2026-04-04T08:00:00Z", distanceMeters = 10000, durationSeconds = 1800),
                activity("2026-04-11T08:00:00Z", distanceMeters = 11000, durationSeconds = 1900),
                activity("2026-04-18T08:00:00Z", distanceMeters = 12000, durationSeconds = 2000),
                activity("2026-04-06T08:00:00Z", distanceMeters = 9000, durationSeconds = 1700),
            ),
        )

        requireNotNull(highlights)
        assertEquals(DayOfWeek.SATURDAY, highlights.favoriteDayOfWeek)
        assertEquals(3, highlights.dayOfWeekDistribution[DayOfWeek.SATURDAY])
        assertEquals(1, highlights.dayOfWeekDistribution[DayOfWeek.MONDAY])
    }

    @Test
    fun `map highlights returns null average travel speed when duration is zero`() {
        val highlights = mapper.mapHighlights(
            listOf(
                activity("2026-04-01T08:00:00Z", distanceMeters = 10000, durationSeconds = 0),
            ),
        )

        requireNotNull(highlights)
        assertNull(highlights.avgTravelSpeedKmh)
    }

    private fun activity(
        startTime: String,
        distanceMeters: Int,
        durationSeconds: Int,
        elevationGainMeters: Int? = null,
        maxSpeedKmh: Double? = null,
        maxRiderPowerWatts: Double? = null,
        caloriesBurned: Double? = null,
    ) = BoschActivity(
        id = startTime,
        title = "Ride",
        startTime = startTime,
        endTime = null,
        timeZone = "Europe/Vienna",
        durationWithoutStopsSeconds = durationSeconds,
        bikeId = null,
        startOdometerMeters = null,
        distanceMeters = distanceMeters,
        averageSpeedKmh = null,
        maxSpeedKmh = maxSpeedKmh,
        averageCadenceRpm = null,
        maxCadenceRpm = null,
        averageRiderPowerWatts = null,
        maxRiderPowerWatts = maxRiderPowerWatts,
        elevationGainMeters = elevationGainMeters,
        elevationLossMeters = null,
        caloriesBurned = caloriesBurned,
    )
}
