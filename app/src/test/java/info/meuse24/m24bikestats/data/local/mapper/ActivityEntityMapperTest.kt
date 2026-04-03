package info.meuse24.m24bikestats.data.local.mapper

import info.meuse24.m24bikestats.data.local.entity.ActivityEntity
import info.meuse24.m24bikestats.domain.model.BoschActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActivityEntityMapperTest {

    @Test
    fun `activity maps to entity with parsed epoch`() {
        val activity = BoschActivity(
            id = "activity-1",
            title = "Morgenrunde",
            startTime = "2026-04-03T10:00:00Z",
            endTime = null,
            timeZone = "Europe/Vienna",
            durationWithoutStopsSeconds = 1800,
            bikeId = "bike-1",
            startOdometerMeters = 12000,
            distanceMeters = 15340,
            averageSpeedKmh = 23.4,
            maxSpeedKmh = 42.1,
            averageCadenceRpm = 79.0,
            maxCadenceRpm = 95.0,
            averageRiderPowerWatts = 201.0,
            maxRiderPowerWatts = 405.0,
            elevationGainMeters = 320,
            elevationLossMeters = 315,
            caloriesBurned = 620.0,
        )

        val entity = activity.toEntity()

        assertEquals("activity-1", entity.id)
        assertEquals(1775210400000L, entity.startTimeEpoch)
        assertEquals(15340, entity.distanceMeters)
        assertEquals(23.4, entity.averageSpeedKmh)
    }

    @Test
    fun `entity maps back to domain`() {
        val entity = ActivityEntity(
            id = "activity-2",
            title = "Abendrunde",
            startTime = "2026-04-03T18:00:00Z",
            startTimeEpoch = 1775239200000L,
            endTime = null,
            timeZone = null,
            durationWithoutStopsSeconds = 1200,
            bikeId = null,
            startOdometerMeters = null,
            distanceMeters = 8900,
            averageSpeedKmh = null,
            maxSpeedKmh = 31.5,
            averageCadenceRpm = null,
            maxCadenceRpm = null,
            averageRiderPowerWatts = null,
            maxRiderPowerWatts = null,
            elevationGainMeters = null,
            elevationLossMeters = null,
            caloriesBurned = null,
        )

        val domain = entity.toDomain()

        assertEquals("activity-2", domain.id)
        assertEquals("Abendrunde", domain.title)
        assertEquals(8900, domain.distanceMeters)
        assertNull(domain.averageSpeedKmh)
        assertEquals(31.5, domain.maxSpeedKmh)
    }
}
