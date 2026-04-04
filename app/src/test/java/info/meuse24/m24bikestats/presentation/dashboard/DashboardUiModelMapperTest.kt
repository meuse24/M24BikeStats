package info.meuse24.m24bikestats.presentation.dashboard

import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.model.BoschActivityDetail
import info.meuse24.m24bikestats.domain.model.BoschActivityDetailPoint
import info.meuse24.m24bikestats.domain.model.BoschAssistMode
import info.meuse24.m24bikestats.domain.model.BoschBattery
import info.meuse24.m24bikestats.domain.model.BoschBike
import info.meuse24.m24bikestats.domain.model.BoschDriveUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardUiModelMapperTest {

    private val mapper = DashboardUiModelMapper(MapperTestStringResolver())

    @Test
    fun `activity detail filters zero coordinates and compresses duplicate points`() {
        val uiModel = mapper.toActivityDetailUiModel(
            activity = activity(),
            detail = BoschActivityDetail(
                activityId = "a1",
                points = listOf(
                    point(distanceMeters = 0.0, latitude = 0.0, longitude = 0.0, altitudeMeters = 405.0),
                    point(distanceMeters = 10.0, latitude = 47.1, longitude = 9.1, altitudeMeters = 406.0),
                    point(distanceMeters = 10.0, latitude = 47.1, longitude = 9.1, altitudeMeters = 407.0, speedKmh = 21.0),
                    point(distanceMeters = 20.0, latitude = 47.2, longitude = 9.2, altitudeMeters = 408.0, speedKmh = 24.0),
                ),
            ),
        )

        assertEquals(2, uiModel.trackPoints.size)
        assertEquals(10.0, uiModel.trackPoints.first().distanceMeters!!, 0.0)
        assertEquals(407.0, uiModel.trackPoints.first().altitudeMeters!!, 0.0)
        assertEquals(listOf(0.0, 10.0, 20.0), uiModel.profilePoints.map { it.distanceMeters })
    }

    @Test
    fun `bike card exposes walk assist power on summary and cleaned assist ranges`() {
        val uiModel = mapper.toBikeCardUiModel(bike())

        assertTrue(uiModel.walkAssistLabel!!.startsWith("res-"))
        assertTrue(uiModel.powerOnSummary!!.contains("867"))
        assertTrue(uiModel.assistModesSummary!!.contains("97"))
        assertTrue(uiModel.assistModesSummary!!.contains("59"))
        assertFalse(uiModel.assistModesSummary!!.contains("0 km"))
    }

    @Test
    fun `bike detail creates dedicated assist mode section without placeholder mode`() {
        val uiModel = mapper.toBikeDetailUiModel(bike())

        val assistSection = uiModel.sections.first { section ->
            section.rows.any { (label, _) -> label == "A100M00040" }
        }

        assertEquals(2, assistSection.rows.size)
        assertEquals("A100M00040", assistSection.rows[0].first)
        assertEquals("A100M00030", assistSection.rows[1].first)
    }

    private fun activity() = BoschActivity(
        id = "a1",
        title = "Ride",
        startTime = "2026-04-03T10:00:00Z",
        endTime = "2026-04-03T11:00:00Z",
        timeZone = "Europe/Vienna",
        durationWithoutStopsSeconds = 3600,
        bikeId = "bike-1",
        startOdometerMeters = 123000,
        distanceMeters = 25000,
        averageSpeedKmh = 25.0,
        maxSpeedKmh = 40.0,
        averageCadenceRpm = 70.0,
        maxCadenceRpm = 90.0,
        averageRiderPowerWatts = 100.0,
        maxRiderPowerWatts = 200.0,
        elevationGainMeters = 100,
        elevationLossMeters = 90,
        caloriesBurned = 300.0,
    )

    private fun bike() = BoschBike(
        id = "bike-1",
        createdAt = "2024-06-14T12:45:12.123452Z",
        language = "de",
        driveUnit = BoschDriveUnit(
            serialNumber = "serial",
            partNumber = "part",
            productName = "Drive Unit Performance Line CX",
            odometerMeters = 6336824.0,
            rearWheelCircumferenceMillimeters = 2260.0,
            maximumAssistanceSpeedKmh = 27.4,
            walkAssistEnabled = true,
            walkAssistMaximumSpeedKmh = 4.0,
            activeAssistModes = listOf(
                BoschAssistMode(name = "0", reachableRangeKm = 0.0),
                BoschAssistMode(name = "A100M00030", reachableRangeKm = 59.0),
                BoschAssistMode(name = "A100M00040", reachableRangeKm = 97.0),
            ),
            totalPowerOnHours = 867,
            supportPowerOnHours = 867,
        ),
        remoteControl = null,
        headUnit = null,
        batteries = listOf(
            BoschBattery(
                serialNumber = "battery-serial",
                partNumber = "battery-part",
                productName = "PowerTube 750",
                deliveredWhOverLifetime = 51554,
                totalChargeCycles = 83.8,
                onBikeChargeCycles = 79.2,
                offBikeChargeCycles = 4.5,
            )
        ),
    )

    private fun point(
        distanceMeters: Double?,
        latitude: Double?,
        longitude: Double?,
        altitudeMeters: Double?,
        speedKmh: Double? = null,
    ) = BoschActivityDetailPoint(
        distanceMeters = distanceMeters,
        altitudeMeters = altitudeMeters,
        speedKmh = speedKmh,
        cadenceRpm = null,
        latitude = latitude,
        longitude = longitude,
        riderPowerWatts = null,
    )
}

private class MapperTestStringResolver : DashboardStringResolver {
    override fun get(resId: Int, args: Array<out Any>): String =
        buildString {
            append("res-")
            append(resId)
            if (args.isNotEmpty()) {
                append(':')
                append(args.joinToString(","))
            }
        }
}
