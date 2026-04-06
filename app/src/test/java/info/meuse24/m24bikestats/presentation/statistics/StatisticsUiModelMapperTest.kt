package info.meuse24.m24bikestats.presentation.statistics

import info.meuse24.m24bikestats.domain.model.BoschActivity
import org.junit.Assert.assertEquals
import org.junit.Test
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

    private fun activity(
        startTime: String,
        distanceMeters: Int,
        durationSeconds: Int,
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
        maxSpeedKmh = null,
        averageCadenceRpm = null,
        maxCadenceRpm = null,
        averageRiderPowerWatts = null,
        maxRiderPowerWatts = null,
        elevationGainMeters = null,
        elevationLossMeters = null,
        caloriesBurned = null,
    )
}
