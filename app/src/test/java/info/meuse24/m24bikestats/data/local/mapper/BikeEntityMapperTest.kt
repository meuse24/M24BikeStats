package info.meuse24.m24bikestats.data.local.mapper

import info.meuse24.m24bikestats.data.local.entity.BikeAssistModeEntity
import info.meuse24.m24bikestats.data.local.entity.BikeBatteryEntity
import info.meuse24.m24bikestats.data.local.entity.BikeEntity
import info.meuse24.m24bikestats.data.local.model.CachedBike
import info.meuse24.m24bikestats.domain.model.BoschAssistMode
import info.meuse24.m24bikestats.domain.model.BoschBattery
import info.meuse24.m24bikestats.domain.model.BoschBike
import info.meuse24.m24bikestats.domain.model.BoschComponent
import info.meuse24.m24bikestats.domain.model.BoschDriveUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BikeEntityMapperTest {

    @Test
    fun `bike maps to entity and related rows`() {
        val bike = BoschBike(
            id = "bike-1",
            createdAt = "2026-04-03T10:00:00Z",
            language = "de",
            driveUnit = BoschDriveUnit(
                serialNumber = "du-1",
                partNumber = "pn-1",
                productName = "Performance Line CX",
                odometerMeters = 12345.0,
                rearWheelCircumferenceMillimeters = 2300.0,
                maximumAssistanceSpeedKmh = 25.0,
                walkAssistEnabled = true,
                walkAssistMaximumSpeedKmh = 6.0,
                activeAssistModes = listOf(BoschAssistMode("Tour+", 75.0)),
                totalPowerOnHours = 12,
                supportPowerOnHours = 10,
            ),
            remoteControl = BoschComponent("rc-1", "rc-pn", "Mini Remote"),
            headUnit = BoschComponent("hu-1", "hu-pn", "Kiox 300"),
            batteries = listOf(BoschBattery("bat-1", "bat-pn", "PowerTube", 20000, 50.0, 40.0, 10.0)),
        )

        val entity = bike.toEntity(updatedAtEpochMillis = 1234L)
        val batteries = bike.toBatteryEntities()
        val assistModes = bike.toAssistModeEntities()

        assertEquals("bike-1", entity.id)
        assertEquals(1234L, entity.updatedAtEpochMillis)
        assertEquals("Performance Line CX", entity.driveUnitProductName)
        assertEquals(1, batteries.size)
        assertEquals(1, assistModes.size)
        assertEquals("Tour+", assistModes.single().name)
    }

    @Test
    fun `cached bike maps back to domain`() {
        val cachedBike = CachedBike(
            bike = BikeEntity(
                id = "bike-1",
                createdAt = "2026-04-03T10:00:00Z",
                updatedAtEpochMillis = 1234L,
                language = "de",
                driveUnitSerialNumber = "du-1",
                driveUnitPartNumber = "pn-1",
                driveUnitProductName = "Performance Line CX",
                driveUnitOdometerMeters = 12345.0,
                driveUnitRearWheelCircumferenceMillimeters = 2300.0,
                driveUnitMaximumAssistanceSpeedKmh = 25.0,
                driveUnitWalkAssistEnabled = true,
                driveUnitWalkAssistMaximumSpeedKmh = 6.0,
                driveUnitTotalPowerOnHours = 12,
                driveUnitSupportPowerOnHours = 10,
                remoteControlSerialNumber = "rc-1",
                remoteControlPartNumber = "rc-pn",
                remoteControlProductName = "Mini Remote",
                headUnitSerialNumber = "hu-1",
                headUnitPartNumber = "hu-pn",
                headUnitProductName = "Kiox 300",
            ),
            batteries = listOf(
                BikeBatteryEntity("bike-1", 0, "bat-1", "bat-pn", "PowerTube", 20000, 50.0, 40.0, 10.0),
            ),
            assistModes = listOf(
                BikeAssistModeEntity("bike-1", 0, "Tour+", 75.0),
            ),
        )

        val domain = cachedBike.toDomain()

        assertEquals("bike-1", domain.id)
        assertEquals("Performance Line CX", domain.driveUnit?.productName)
        assertEquals(1, domain.batteries.size)
        assertEquals("PowerTube", domain.batteries.single().productName)
        assertTrue(domain.driveUnit?.activeAssistModes?.any { it.name == "Tour+" } == true)
    }
}
