package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.BoschActivityDetail
import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.model.BoschActivityPage
import info.meuse24.m24bikestats.domain.model.BoschBike
import info.meuse24.m24bikestats.domain.model.CsvExportFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class RefreshSmartSystemUseCasesTest {

    @Test
    fun `activities refresh skips remote call when cache is fresh and not forced`() {
        val repository = FakeBoschSmartSystemRepository().apply {
            activitiesFresh = true
        }
        val useCase = RefreshSmartSystemActivitiesUseCase(
            repository = repository,
            authRepository = FakeAuthRepository(),
            cacheTtlMillis = 123L,
        )

        val result = kotlinx.coroutines.runBlocking { useCase(limit = 20, offset = 0, force = false) }

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
        assertTrue(repository.getActivitiesCalls.isEmpty())
    }

    @Test
    fun `activities refresh loads remote when forced`() {
        val page = BoschActivityPage(total = 42, offset = 0, limit = 20, items = emptyList())
        val repository = FakeBoschSmartSystemRepository().apply {
            activitiesFresh = true
            activitiesResult = Result.success(page)
        }
        val useCase = RefreshSmartSystemActivitiesUseCase(
            repository = repository,
            authRepository = FakeAuthRepository(),
        )

        val result = kotlinx.coroutines.runBlocking { useCase(limit = 20, offset = 0, force = true) }

        assertEquals(page, result.getOrNull())
        assertEquals(listOf(20 to 0), repository.getActivitiesCalls)
    }

    @Test
    fun `activity detail refresh skips remote call when cache is fresh`() {
        val repository = FakeBoschSmartSystemRepository().apply {
            activityDetailFresh = true
        }
        val useCase = RefreshSmartSystemActivityDetailUseCase(
            repository = repository,
            authRepository = FakeAuthRepository(),
            cacheTtlMillis = 456L,
        )

        val result = kotlinx.coroutines.runBlocking { useCase(activityId = "a1", force = false) }

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
        assertTrue(repository.getActivityDetailCalls.isEmpty())
    }

    @Test
    fun `bike list refresh loads remote when cache is stale`() {
        val bikes = listOf(
            BoschBike(
                id = "bike-1",
                createdAt = null,
                language = null,
                driveUnit = null,
                remoteControl = null,
                headUnit = null,
                batteries = emptyList(),
            )
        )
        val repository = FakeBoschSmartSystemRepository().apply {
            bikesFresh = false
            bikesResult = Result.success(bikes)
        }
        val useCase = RefreshSmartSystemBikesUseCase(
            repository = repository,
            authRepository = FakeAuthRepository(),
        )

        val result = kotlinx.coroutines.runBlocking { useCase(force = false) }

        assertEquals(bikes, result.getOrNull())
        assertEquals(1, repository.getBikesCalls)
    }

    @Test
    fun `bike detail refresh loads remote when forced even if cache is fresh`() {
        val bike = BoschBike(
            id = "bike-1",
            createdAt = null,
            language = null,
            driveUnit = null,
            remoteControl = null,
            headUnit = null,
            batteries = emptyList(),
        )
        val repository = FakeBoschSmartSystemRepository().apply {
            bikeDetailFresh = true
            bikeDetailResult = Result.success(bike)
        }
        val useCase = RefreshSmartSystemBikeDetailUseCase(
            repository = repository,
            authRepository = FakeAuthRepository(),
        )

        val result = kotlinx.coroutines.runBlocking { useCase(bikeId = "bike-1", force = true) }

        assertNotNull(result.getOrNull())
        assertEquals(listOf("bike-1"), repository.getBikeDetailCalls)
    }

    @Test
    fun `auth failure is returned before remote refresh`() {
        val repository = FakeBoschSmartSystemRepository()
        val useCase = RefreshSmartSystemBikesUseCase(
            repository = repository,
            authRepository = FakeAuthRepository(Result.failure(IllegalStateException("auth failed"))),
        )

        val result = kotlinx.coroutines.runBlocking { useCase(force = true) }

        assertTrue(result.isFailure)
        assertEquals(0, repository.getBikesCalls)
    }

    @Test
    fun `csv export fetches first page when cache is empty and total unknown`() {
        val page = BoschActivityPage(
            total = 1,
            offset = 0,
            limit = 100,
            items = listOf(
                info.meuse24.m24bikestats.domain.model.BoschActivity(
                    id = "a1",
                    title = "Ride",
                    startTime = "2026-04-03T10:00:00Z",
                    endTime = null,
                    timeZone = null,
                    durationWithoutStopsSeconds = 1200,
                    bikeId = null,
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
            ),
        )
        val repository = FakeBoschSmartSystemRepository().apply {
            cachedActivities = emptyList()
            cachedActivityTotalCount = null
            activitiesResult = Result.success(page)
        }
        val useCase = ExportSmartSystemActivitiesCsvUseCase(
            repository = repository,
            authRepository = FakeAuthRepository(),
            appSettingsRepository = FakeAppSettingsRepository(),
        )

        val result = kotlinx.coroutines.runBlocking { useCase() }

        assertTrue(result.isSuccess)
        assertEquals(listOf(100 to 0), repository.getActivitiesCalls)
        assertEquals(1, result.getOrNull()?.activityCount)
    }

    @Test
    fun `csv export skips remote when cache already complete`() {
        val repository = FakeBoschSmartSystemRepository().apply {
            cachedActivities = listOf(
                info.meuse24.m24bikestats.domain.model.BoschActivity(
                    id = "a1",
                    title = "Ride",
                    startTime = "2026-04-03T10:00:00Z",
                    endTime = null,
                    timeZone = null,
                    durationWithoutStopsSeconds = 1200,
                    bikeId = null,
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
            cachedActivityTotalCount = 1
        }
        val useCase = ExportSmartSystemActivitiesCsvUseCase(
            repository = repository,
            authRepository = FakeAuthRepository(),
            appSettingsRepository = FakeAppSettingsRepository(),
        )

        val result = kotlinx.coroutines.runBlocking { useCase() }

        assertTrue(result.isSuccess)
        assertTrue(repository.getActivitiesCalls.isEmpty())
        assertEquals(1, result.getOrNull()?.activityCount)
    }

    @Test
    fun `csv export uses german excel preset for decimal numbers`() {
        val repository = FakeBoschSmartSystemRepository().apply {
            cachedActivities = listOf(
                BoschActivity(
                    id = "a1",
                    title = "Ride",
                    startTime = "2026-04-03T10:00:00Z",
                    endTime = null,
                    timeZone = null,
                    durationWithoutStopsSeconds = 1200,
                    bikeId = null,
                    startOdometerMeters = null,
                    distanceMeters = 1234,
                    averageSpeedKmh = 24.5,
                    maxSpeedKmh = null,
                    averageCadenceRpm = null,
                    maxCadenceRpm = null,
                    averageRiderPowerWatts = null,
                    maxRiderPowerWatts = null,
                    elevationGainMeters = null,
                    elevationLossMeters = null,
                    caloriesBurned = 321.9,
                )
            )
            cachedActivityTotalCount = 1
        }
        val useCase = ExportSmartSystemActivitiesCsvUseCase(
            repository = repository,
            authRepository = FakeAuthRepository(),
            appSettingsRepository = FakeAppSettingsRepository(CsvExportFormat.EXCEL_DE),
            localeProvider = { Locale.GERMAN },
        )

        val result = kotlinx.coroutines.runBlocking { useCase() }

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.csvContent.contains("\"03.04.2026 10:00:00\""))
        assertTrue(result.getOrNull()!!.csvContent.contains("\"24,50\""))
        assertTrue(result.getOrNull()!!.csvContent.contains("\"321,90\""))
    }

    @Test
    fun `cloud sync fetches all pages and bikes`() {
        val repository = FakeBoschSmartSystemRepository().apply {
            bikesResult = Result.success(
                listOf(
                    BoschBike(
                        id = "bike-1",
                        createdAt = null,
                        language = null,
                        driveUnit = null,
                        remoteControl = null,
                        headUnit = null,
                        batteries = emptyList(),
                    )
                )
            )
            activityResultsByOffset[0] = Result.success(
                BoschActivityPage(
                    total = 3,
                    offset = 0,
                    limit = 100,
                    items = listOf(activity("a1"), activity("a2")),
                )
            )
            activityResultsByOffset[2] = Result.success(
                BoschActivityPage(
                    total = 3,
                    offset = 2,
                    limit = 100,
                    items = listOf(activity("a3")),
                )
            )
        }
        val progressEvents = mutableListOf<Pair<Int, Int>>()
        val useCase = SyncSmartSystemCloudUseCase(
            repository = repository,
            authRepository = FakeAuthRepository(),
        )

        val result = kotlinx.coroutines.runBlocking {
            useCase { loaded, total -> progressEvents += loaded to total }
        }

        assertTrue(result.isSuccess)
        assertEquals(listOf(100 to 0, 100 to 2), repository.getActivitiesCalls)
        assertEquals(1, repository.getBikesCalls)
        assertEquals(listOf(2 to 3, 3 to 3), progressEvents)
        assertEquals(3, result.getOrNull()?.activityCount)
        assertEquals(1, result.getOrNull()?.bikeCount)
    }

    private fun activity(id: String) = BoschActivity(
        id = id,
        title = "Ride $id",
        startTime = "2026-04-03T10:00:00Z",
        endTime = null,
        timeZone = null,
        durationWithoutStopsSeconds = 1200,
        bikeId = null,
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
}
