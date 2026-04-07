package info.meuse24.m24bikestats.data.local.mapper

import info.meuse24.m24bikestats.data.local.entity.BikeAssistModeEntity
import info.meuse24.m24bikestats.data.local.entity.BikeAbsEntity
import info.meuse24.m24bikestats.data.local.entity.BikeBatteryEntity
import info.meuse24.m24bikestats.data.local.entity.BikeEntity
import info.meuse24.m24bikestats.data.local.entity.BikePassEntity
import info.meuse24.m24bikestats.data.local.entity.BikeRegistrationEntity
import info.meuse24.m24bikestats.data.local.entity.BikeServiceRecordEntity
import info.meuse24.m24bikestats.data.local.entity.BikeTheftReportLogEntity
import info.meuse24.m24bikestats.data.local.model.CachedBike
import info.meuse24.m24bikestats.domain.model.BoschAssistMode
import info.meuse24.m24bikestats.domain.model.BoschBattery
import info.meuse24.m24bikestats.domain.model.BoschBatteryMeasurement
import info.meuse24.m24bikestats.domain.model.BoschBike
import info.meuse24.m24bikestats.domain.model.BoschBikePass
import info.meuse24.m24bikestats.domain.model.BoschComponent
import info.meuse24.m24bikestats.domain.model.BoschDriveUnit
import info.meuse24.m24bikestats.domain.model.BoschRegistration
import info.meuse24.m24bikestats.domain.model.BoschServiceRecord
import info.meuse24.m24bikestats.domain.model.BoschSoftwareUpdateSummary
import info.meuse24.m24bikestats.domain.model.BoschTheftLocation
import info.meuse24.m24bikestats.domain.model.BoschTheftReportLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BikeEntityMapperTest {

    @Test
    fun `bike maps to entity and related rows`() {
        val bike = BoschBike(
            id = "bike-1",
            createdAt = "2026-04-03T10:00:00Z",
            language = "de",
            oemId = "oem-1",
            serviceDueDate = "2026-06-01T10:00:00Z",
            serviceDueOdometerMeters = 200000.0,
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
            connectModule = BoschComponent("cm-1", "cm-pn", "ConnectModule"),
            antiLockBrakeSystems = listOf(BoschComponent("abs-1", "abs-pn", "eBike ABS")),
            bikePass = BoschBikePass(
                bikeId = "bike-1",
                frameNumber = "FRAME-123",
                frameNumberPosition = "bottom bracket",
                description = "orange fork",
                createdAt = "2026-04-01T10:00:00Z",
                updatedAt = "2026-04-02T10:00:00Z",
            ),
            theftReportLogs = listOf(
                BoschTheftReportLog(
                    theftReportLogId = "log-1",
                    bikeId = "bike-1",
                    createdAt = "2026-04-03T10:00:00Z",
                    expiresAtEpochMillis = 1_800_000L,
                    timeZone = "Europe/Vienna",
                    theftCaseEnteredAt = "2026-04-03T11:00:00Z",
                    riderPortalLink = "https://example.com/theft/1",
                    description = "reported",
                    location = BoschTheftLocation(
                        detectedAt = "2026-04-03T12:00:00Z",
                        latitude = 47.1,
                        longitude = 9.1,
                        horizontalAccuracyMeters = 15.0,
                        address = "Test Street 1",
                        description = "behind stairs",
                    ),
                )
            ),
            serviceRecords = listOf(
                BoschServiceRecord(
                    id = "service-1",
                    type = "DIGITAL_SERVICE",
                    bikeId = "bike-1",
                    createdAt = "2026-04-04T10:00:00Z",
                    odometerValueMeters = 150000L,
                    bikeDealerName = "Dealer One",
                    bikeDealerCity = "Vienna",
                    toolVersion = "5.4.0",
                    batteryMeasurement = BoschBatteryMeasurement(
                        fullChargeCycles = 52,
                        measuredEnergyCapacityWh = 710,
                        nominalEnergyCapacityWh = 750,
                        measuredCapacityPercentage = 95,
                        onBikeMeasurement = false,
                    ),
                    softwareUpdate = BoschSoftwareUpdateSummary(
                        clientType = "DIAGNOSTIC_TOOL",
                        clientVersion = "2026.4",
                        isForcedUpdate = true,
                        updatedComponentsCount = 2,
                        updatedComponentNames = listOf("Drive Unit", "ABS"),
                    ),
                )
            ),
            registrations = listOf(
                BoschRegistration(
                    bikeId = "bike-1",
                    registrationType = "COMPONENT_REGISTRATION",
                    createdAt = "2026-04-05T10:00:00Z",
                    componentType = "BATTERY",
                    partNumber = "bat-pn",
                    serialNumber = "bat-1",
                )
            ),
            batteries = listOf(BoschBattery("bat-1", "bat-pn", "PowerTube", 20000, 50.0, 40.0, 10.0)),
        )

        val entity = bike.toEntity(updatedAtEpochMillis = 1234L)
        val batteries = bike.toBatteryEntities()
        val assistModes = bike.toAssistModeEntities()
        val absComponents = bike.toAbsEntities()
        val bikePass = bike.toBikePassEntity()
        val theftLogs = bike.toTheftReportLogEntities()
        val serviceRecords = bike.toServiceRecordEntities()
        val registrations = bike.toRegistrationEntities()

        assertEquals("bike-1", entity.id)
        assertEquals(1234L, entity.updatedAtEpochMillis)
        assertEquals("oem-1", entity.oemId)
        assertEquals("Performance Line CX", entity.driveUnitProductName)
        assertEquals(1, batteries.size)
        assertEquals(1, assistModes.size)
        assertEquals(1, absComponents.size)
        assertEquals("FRAME-123", bikePass!!.frameNumber)
        assertEquals(1, theftLogs.size)
        assertEquals(1, serviceRecords.size)
        assertEquals(1, registrations.size)
        assertEquals("Dealer One", serviceRecords.single().bikeDealerName)
        assertEquals("BATTERY", registrations.single().componentType)
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
                oemId = "oem-1",
                serviceDueDate = "2026-06-01T10:00:00Z",
                serviceDueOdometerMeters = 200000.0,
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
                connectModuleSerialNumber = "cm-1",
                connectModulePartNumber = "cm-pn",
                connectModuleProductName = "ConnectModule",
            ),
            batteries = listOf(
                BikeBatteryEntity("bike-1", 0, "bat-1", "bat-pn", "PowerTube", 20000, 50.0, 40.0, 10.0),
            ),
            assistModes = listOf(
                BikeAssistModeEntity("bike-1", 0, "Tour+", 75.0),
            ),
            antiLockBrakeSystems = listOf(
                BikeAbsEntity("bike-1", 0, "abs-1", "abs-pn", "eBike ABS"),
            ),
            bikePass = BikePassEntity(
                bikeId = "bike-1",
                frameNumber = "FRAME-123",
                frameNumberPosition = "bottom bracket",
                description = "orange fork",
                createdAt = "2026-04-01T10:00:00Z",
                updatedAt = "2026-04-02T10:00:00Z",
            ),
            theftReportLogs = listOf(
                BikeTheftReportLogEntity(
                    bikeId = "bike-1",
                    theftReportLogId = "log-1",
                    createdAt = "2026-04-03T10:00:00Z",
                    expiresAtEpochMillis = 1_800_000L,
                    timeZone = "Europe/Vienna",
                    theftCaseEnteredAt = "2026-04-03T11:00:00Z",
                    riderPortalLink = "https://example.com/theft/1",
                    description = "reported",
                    locationDetectedAt = "2026-04-03T12:00:00Z",
                    locationLatitude = 47.1,
                    locationLongitude = 9.1,
                    locationHorizontalAccuracyMeters = 15.0,
                    locationAddress = "Test Street 1",
                    locationDescription = "behind stairs",
                )
            ),
            serviceRecords = listOf(
                BikeServiceRecordEntity(
                    bikeId = "bike-1",
                    serviceRecordId = "service-1",
                    type = "DIGITAL_SERVICE",
                    createdAt = "2026-04-04T10:00:00Z",
                    odometerValueMeters = 150000L,
                    bikeDealerName = "Dealer One",
                    bikeDealerCity = "Vienna",
                    toolVersion = "5.4.0",
                    batteryFullChargeCycles = 52,
                    batteryMeasuredEnergyCapacityWh = 710,
                    batteryNominalEnergyCapacityWh = 750,
                    batteryMeasuredCapacityPercentage = 95,
                    batteryOnBikeMeasurement = false,
                    softwareUpdateClientType = "DIAGNOSTIC_TOOL",
                    softwareUpdateClientVersion = "2026.4",
                    softwareUpdateForced = true,
                    softwareUpdateUpdatedComponentsCount = 2,
                    softwareUpdateUpdatedComponentNames = "Drive Unit|ABS",
                )
            ),
            registrations = listOf(
                BikeRegistrationEntity(
                    bikeId = "bike-1",
                    registrationKey = "COMPONENT_REGISTRATION|2026-04-05T10:00:00Z|BATTERY|bat-pn|bat-1",
                    registrationType = "COMPONENT_REGISTRATION",
                    createdAt = "2026-04-05T10:00:00Z",
                    componentType = "BATTERY",
                    partNumber = "bat-pn",
                    serialNumber = "bat-1",
                )
            ),
        )

        val domain = cachedBike.toDomain()

        assertEquals("bike-1", domain.id)
        assertEquals("oem-1", domain.oemId)
        assertEquals("Performance Line CX", domain.driveUnit?.productName)
        assertEquals("ConnectModule", domain.connectModule?.productName)
        assertEquals("eBike ABS", domain.antiLockBrakeSystems.single().productName)
        assertEquals("FRAME-123", domain.bikePass!!.frameNumber)
        assertEquals("reported", domain.theftReportLogs.single().description)
        assertEquals("Dealer One", domain.serviceRecords.single().bikeDealerName)
        assertEquals("ABS", domain.serviceRecords.single().softwareUpdate!!.updatedComponentNames.last())
        assertEquals("BATTERY", domain.registrations.single().componentType)
        assertEquals(1, domain.batteries.size)
        assertEquals("PowerTube", domain.batteries.single().productName)
        assertTrue(domain.driveUnit?.activeAssistModes?.any { it.name == "Tour+" } == true)
    }

    @Test
    fun `cached bike without drive unit fields maps to null drive unit`() {
        val cachedBike = CachedBike(
            bike = BikeEntity(
                id = "bike-2",
                createdAt = "2026-04-03T10:00:00Z",
                updatedAtEpochMillis = 1234L,
                language = "de",
                oemId = null,
                serviceDueDate = null,
                serviceDueOdometerMeters = null,
                driveUnitSerialNumber = null,
                driveUnitPartNumber = null,
                driveUnitProductName = null,
                driveUnitOdometerMeters = null,
                driveUnitRearWheelCircumferenceMillimeters = null,
                driveUnitMaximumAssistanceSpeedKmh = null,
                driveUnitWalkAssistEnabled = null,
                driveUnitWalkAssistMaximumSpeedKmh = null,
                driveUnitTotalPowerOnHours = null,
                driveUnitSupportPowerOnHours = null,
                remoteControlSerialNumber = null,
                remoteControlPartNumber = null,
                remoteControlProductName = null,
                headUnitSerialNumber = null,
                headUnitPartNumber = null,
                headUnitProductName = null,
                connectModuleSerialNumber = null,
                connectModulePartNumber = null,
                connectModuleProductName = null,
            ),
            batteries = emptyList(),
            assistModes = emptyList(),
            antiLockBrakeSystems = emptyList(),
            bikePass = null,
            theftReportLogs = emptyList(),
            serviceRecords = emptyList(),
            registrations = emptyList(),
        )

        val domain = cachedBike.toDomain()

        assertEquals("bike-2", domain.id)
        assertNull(domain.driveUnit)
    }
}
