package info.meuse24.m24bikestats.data.local.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import info.meuse24.m24bikestats.data.local.entity.ActivityCacheStateEntity
import info.meuse24.m24bikestats.data.local.entity.ActivityDetailEntity
import info.meuse24.m24bikestats.data.local.entity.ActivityDetailPointEntity
import info.meuse24.m24bikestats.data.local.entity.ActivityEntity
import info.meuse24.m24bikestats.data.local.entity.BikeAssistModeEntity
import info.meuse24.m24bikestats.data.local.entity.BikeBatteryEntity
import info.meuse24.m24bikestats.data.local.entity.BikeCacheStateEntity
import info.meuse24.m24bikestats.data.local.entity.BikeEntity
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
                )
            ),
            batteries = listOf(
                BikeBatteryEntity("bike-1", 0, "bat-1", null, "PowerTube", 20000, 50.0, 40.0, 10.0)
            ),
            assistModes = listOf(
                BikeAssistModeEntity("bike-1", 0, "Tour+", 80.0)
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
