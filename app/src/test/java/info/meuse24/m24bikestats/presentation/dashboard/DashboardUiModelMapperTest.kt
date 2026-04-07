package info.meuse24.m24bikestats.presentation.dashboard

import info.meuse24.m24bikestats.auth.OidcCertificateInfoUiModel
import info.meuse24.m24bikestats.auth.OidcDiscoveryInfoUiModel
import info.meuse24.m24bikestats.auth.OidcUserInfoUiModel
import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.model.BoschActivityDetail
import info.meuse24.m24bikestats.domain.model.BoschActivityDetailPoint
import info.meuse24.m24bikestats.domain.model.BoschAssistMode
import info.meuse24.m24bikestats.domain.model.BoschBattery
import info.meuse24.m24bikestats.domain.model.BoschBatteryMeasurement
import info.meuse24.m24bikestats.domain.model.BoschBike
import info.meuse24.m24bikestats.domain.model.BoschBikePass
import info.meuse24.m24bikestats.domain.model.BoschDriveUnit
import info.meuse24.m24bikestats.domain.model.BoschRegistration
import info.meuse24.m24bikestats.domain.model.BoschServiceRecord
import info.meuse24.m24bikestats.domain.model.BoschSoftwareUpdateSummary
import info.meuse24.m24bikestats.domain.model.BoschTheftLocation
import info.meuse24.m24bikestats.domain.model.BoschTheftReportLog
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
        assertTrue(uiModel.batterySummary!!.contains("82"))
        assertTrue(uiModel.bikePassSummary!!.contains("FRAME-123"))
        assertTrue(uiModel.assistModesSummary!!.contains("97"))
        assertTrue(uiModel.assistModesSummary!!.contains("59"))
        assertFalse(uiModel.assistModesSummary!!.contains("0 km"))
        assertTrue(uiModel.shareText.contains("Drive Unit Performance Line CX"))
        assertTrue(uiModel.shareText.contains("PowerTube 750"))
        assertTrue(uiModel.shareText.contains("A100M00040"))
        assertTrue(uiModel.shareText.contains("OEM-4711"))
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

    @Test
    fun `bike detail includes service due connect module and abs sections`() {
        val uiModel = mapper.toBikeDetailUiModel(bike())

        assertTrue(uiModel.sections.any { section -> section.rows.any { (_, value) -> value == "OEM-4711" } })
        assertTrue(uiModel.sections.any { section -> section.rows.any { (_, value) -> value == "ConnectModule" } })
        assertTrue(uiModel.sections.any { section -> section.rows.any { (_, value) -> value == "eBike ABS" } })
        assertTrue(uiModel.sections.any { section -> section.rows.any { (_, value) -> value == "FRAME-123" } })
        assertTrue(uiModel.sections.any { section -> section.rows.any { (_, value) -> value == "https://example.com/theft/1" } })
        assertTrue(uiModel.sections.any { section -> section.rows.any { (_, value) -> value == "Dealer One, Vienna" } })
        assertTrue(uiModel.sections.any { section -> section.rows.any { (_, value) -> value.contains("Drive Unit, ABS") } })
        assertTrue(uiModel.sections.any { section -> section.rows.any { (_, value) -> value.contains("BATTERY") } })
        assertTrue(uiModel.sections.any { section -> section.rows.any { (_, value) -> value == "bat-pn" } })
    }

    @Test
    fun `bike detail creates battery card with health indicator`() {
        val uiModel = mapper.toBikeDetailUiModel(bike())

        val batterySection = uiModel.sections.first { section ->
            section.rows.any { (_, value) -> value.contains("82 %") }
        }

        assertTrue(batterySection.title.startsWith("res-"))
        assertTrue(batterySection.title.endsWith(":1"))
        assertEquals(0.82f, batterySection.indicator!!.progress, 0.001f)
        assertTrue(batterySection.indicator!!.supportingText!!.contains("•"))
        assertTrue(batterySection.rows.any { (_, value) -> value.contains("PowerTube 750") })
        assertTrue(batterySection.rows.any { (_, value) -> value.contains("83.8") })
    }

    @Test
    fun `bike detail adds smart system support and oidc certificate section when available`() {
        val uiModel = mapper.toBikeDetailUiModel(
            bike = bike(),
            oidcCertificateInfo = OidcCertificateInfoUiModel(
                tokenKeyId = "token-kid",
                keyId = "active-kid",
                matchesCurrentToken = true,
                keyType = "RSA",
                algorithm = "RS256",
                usage = "sig",
                subject = "CN=Bosch",
                issuer = "CN=Bosch Issuer",
                validFrom = "01.04.2026 10:00",
                validUntil = "01.04.2027 10:00",
                sha1Thumbprint = "sha1",
                sha256Thumbprint = "sha256",
                certificateChainEntries = 2,
            ),
        )

        assertEquals(2, uiModel.sections.first().rows.size)
        assertTrue(uiModel.sections.any { section -> section.rows.any { (_, value) -> value == "active-kid" } })
        assertTrue(uiModel.sections.any { section -> section.rows.any { (_, value) -> value == "CN=Bosch" } })
    }

    @Test
    fun `bike detail adds oidc userinfo and discovery sections when available`() {
        val uiModel = mapper.toBikeDetailUiModel(
            bike = bike(),
            oidcUserInfo = OidcUserInfoUiModel(
                email = "rider@example.com",
                username = "rider",
                subject = "user-subject",
            ),
            oidcDiscoveryInfo = OidcDiscoveryInfoUiModel(
                issuer = "https://issuer.example.com",
                authorizationEndpoint = "https://issuer.example.com/auth",
                tokenEndpoint = "https://issuer.example.com/token",
                userInfoEndpoint = "https://issuer.example.com/userinfo",
                jwksUri = "https://issuer.example.com/jwks",
                revocationEndpoint = "https://issuer.example.com/revoke",
                introspectionEndpoint = "https://issuer.example.com/introspect",
                endSessionEndpoint = "https://issuer.example.com/logout",
                supportedGrantTypes = listOf("authorization_code", "refresh_token"),
            ),
        )

        assertTrue(uiModel.sections.any { section -> section.rows.any { (_, value) -> value == "rider@example.com" } })
        assertTrue(uiModel.sections.any { section -> section.rows.any { (_, value) -> value == "rider" } })
        assertTrue(uiModel.sections.any { section -> section.rows.any { (_, value) -> value == "https://issuer.example.com/token" } })
        assertTrue(uiModel.sections.any { section ->
            section.rows.any { (_, value) -> value == "authorization_code, refresh_token" }
        })
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
        oemId = "OEM-4711",
        serviceDueDate = "2026-08-01T08:00:00Z",
        serviceDueOdometerMeters = 7000000.0,
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
        connectModule = info.meuse24.m24bikestats.domain.model.BoschComponent(
            serialNumber = "connect-serial",
            partNumber = "connect-part",
            productName = "ConnectModule",
        ),
        antiLockBrakeSystems = listOf(
            info.meuse24.m24bikestats.domain.model.BoschComponent(
                serialNumber = "abs-serial",
                partNumber = "abs-part",
                productName = "eBike ABS",
            )
        ),
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
                registrationType = "BIKE_REGISTRATION",
                createdAt = "2026-04-05T10:00:00Z",
                componentType = null,
                partNumber = null,
                serialNumber = null,
            ),
            BoschRegistration(
                bikeId = "bike-1",
                registrationType = "COMPONENT_REGISTRATION",
                createdAt = "2026-04-05T10:00:00Z",
                componentType = "BATTERY",
                partNumber = "bat-pn",
                serialNumber = "battery-serial",
            ),
        ),
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
