package info.meuse24.m24bikestats.data.local.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import info.meuse24.m24bikestats.data.local.entity.ActivityCacheStateEntity
import info.meuse24.m24bikestats.data.local.entity.ActivityDetailEntity
import info.meuse24.m24bikestats.data.local.entity.ActivityDetailPointEntity
import info.meuse24.m24bikestats.data.local.entity.ActivityEntity
import info.meuse24.m24bikestats.data.local.entity.BikeAssistModeEntity
import info.meuse24.m24bikestats.data.local.entity.BikeAbsEntity
import info.meuse24.m24bikestats.data.local.entity.BikeBatteryEntity
import info.meuse24.m24bikestats.data.local.entity.BikeCacheStateEntity
import info.meuse24.m24bikestats.data.local.entity.BikeEntity
import info.meuse24.m24bikestats.data.local.entity.BikePassEntity
import info.meuse24.m24bikestats.data.local.entity.BikeRegistrationEntity
import info.meuse24.m24bikestats.data.local.entity.BikeServiceRecordEntity
import info.meuse24.m24bikestats.data.local.entity.BikeTheftReportLogEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BoschDatabaseTest {

    private lateinit var database: BoschDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BoschDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun activityCacheState_roundTripsTotalCount() = runBlocking {
        database.activityDao().upsertCacheState(
            ActivityCacheStateEntity(totalCount = 453, updatedAtEpochMillis = 1234L)
        )

        assertEquals(453, database.activityDao().getCachedTotalCount())
        assertEquals(1234L, database.activityDao().getCacheUpdatedAtEpochMillis())
    }

    @Test
    fun replaceDetail_replacesOldPointsAndReturnsCurrentRelation() = runBlocking {
        val activityId = "activity-1"
        database.activityDao().upsertAll(
            listOf(
                ActivityEntity(
                    id = activityId,
                    title = "Ride",
                    startTime = "2026-04-03T10:00:00Z",
                    startTimeEpoch = 1L,
                    endTime = null,
                    timeZone = null,
                    durationWithoutStopsSeconds = 1200,
                    bikeId = "bike-1",
                    startOdometerMeters = null,
                    distanceMeters = 1234,
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
            )
        )

        database.activityDetailDao().replaceDetail(
            detail = ActivityDetailEntity(
                activityId = activityId,
                pointCount = 2,
                gpsPointCount = 2,
                updatedAtEpochMillis = 100L,
            ),
            points = listOf(
                ActivityDetailPointEntity(activityId, 0, 0.0, 500.0, null, null, 47.0, 9.0, null),
                ActivityDetailPointEntity(activityId, 1, 100.0, 505.0, null, null, 47.1, 9.1, null),
            ),
        )

        database.activityDetailDao().replaceDetail(
            detail = ActivityDetailEntity(
                activityId = activityId,
                pointCount = 1,
                gpsPointCount = 1,
                updatedAtEpochMillis = 200L,
            ),
            points = listOf(
                ActivityDetailPointEntity(activityId, 0, 0.0, 600.0, null, null, 48.0, 10.0, null),
            ),
        )

        val cached = database.activityDetailDao().getByActivityId(activityId)

        assertNotNull(cached)
        assertEquals(1, cached!!.detail.pointCount)
        assertEquals(1, cached.points.size)
        assertEquals(600.0, cached.points.single().altitudeMeters)
        assertEquals(200L, database.activityDetailDao().getUpdatedAtEpochMillis(activityId))
    }

    @Test
    fun bikeDao_returnsBikeWithRelationsAndCacheState() = runBlocking {
        database.bikeDao().replaceAll(
            bikes = listOf(
                BikeEntity(
                    id = "bike-1",
                    createdAt = "2026-04-03T10:00:00Z",
                    updatedAtEpochMillis = 777L,
                    language = "de",
                    oemId = "OEM-1",
                    serviceDueDate = "2026-06-01T10:00:00Z",
                    serviceDueOdometerMeters = 200000.0,
                    driveUnitSerialNumber = "du-1",
                    driveUnitPartNumber = null,
                    driveUnitProductName = "CX",
                    driveUnitOdometerMeters = 12345.0,
                    driveUnitRearWheelCircumferenceMillimeters = null,
                    driveUnitMaximumAssistanceSpeedKmh = 25.0,
                    driveUnitWalkAssistEnabled = true,
                    driveUnitWalkAssistMaximumSpeedKmh = null,
                    driveUnitTotalPowerOnHours = 10,
                    driveUnitSupportPowerOnHours = 8,
                    remoteControlSerialNumber = null,
                    remoteControlPartNumber = null,
                    remoteControlProductName = null,
                    headUnitSerialNumber = null,
                    headUnitPartNumber = null,
                    headUnitProductName = "Kiox 300",
                    connectModuleSerialNumber = "cm-1",
                    connectModulePartNumber = null,
                    connectModuleProductName = "ConnectModule",
                )
            ),
            batteries = listOf(
                BikeBatteryEntity("bike-1", 0, "bat-1", null, "PowerTube", 20000, 50.0, 40.0, 10.0)
            ),
            assistModes = listOf(
                BikeAssistModeEntity("bike-1", 0, "Tour+", 80.0)
            ),
            absComponents = listOf(
                BikeAbsEntity("bike-1", 0, "abs-1", null, "eBike ABS")
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
            cacheState = BikeCacheStateEntity(updatedAtEpochMillis = 999L),
        )

        val cachedBike = database.bikeDao().getById("bike-1")
        val observedBike = database.bikeDao().observeById("bike-1").first()

        assertNotNull(cachedBike)
        assertEquals(1, cachedBike!!.batteries.size)
        assertEquals("PowerTube", cachedBike.batteries.single().productName)
        assertEquals(1, cachedBike.assistModes.size)
        assertEquals("Tour+", cachedBike.assistModes.single().name)
        assertEquals(1, cachedBike.antiLockBrakeSystems.size)
        assertEquals("eBike ABS", cachedBike.antiLockBrakeSystems.single().productName)
        assertEquals("FRAME-123", cachedBike.bikePass!!.frameNumber)
        assertEquals(1, cachedBike.theftReportLogs.size)
        assertEquals(1, cachedBike.serviceRecords.size)
        assertEquals(1, cachedBike.registrations.size)
        assertEquals("Dealer One", cachedBike.serviceRecords.single().bikeDealerName)
        assertEquals("BATTERY", cachedBike.registrations.single().componentType)
        assertNotNull(observedBike)
        assertEquals(999L, database.bikeDao().getCacheUpdatedAtEpochMillis())
        assertEquals(777L, database.bikeDao().getUpdatedAtEpochMillis("bike-1"))
    }

    @Test
    fun deletingActivity_cascadesToActivityDetailsAndPoints() = runBlocking {
        val activityId = "activity-1"
        database.activityDao().upsertAll(
            listOf(
                ActivityEntity(
                    id = activityId,
                    title = "Ride",
                    startTime = "2026-04-03T10:00:00Z",
                    startTimeEpoch = 1L,
                    endTime = null,
                    timeZone = null,
                    durationWithoutStopsSeconds = 1200,
                    bikeId = "bike-1",
                    startOdometerMeters = null,
                    distanceMeters = 1234,
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
            )
        )
        database.activityDetailDao().replaceDetail(
            detail = ActivityDetailEntity(activityId, 1, 1, 100L),
            points = listOf(
                ActivityDetailPointEntity(activityId, 0, 0.0, 500.0, null, null, 47.0, 9.0, null)
            ),
        )

        database.activityDao().replaceAll(emptyList())

        assertNull(database.activityDetailDao().getByActivityId(activityId))
    }
}
