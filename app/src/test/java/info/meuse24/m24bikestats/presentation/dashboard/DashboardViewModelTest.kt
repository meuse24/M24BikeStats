package info.meuse24.m24bikestats.presentation.dashboard

import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.model.BoschActivityDetail
import info.meuse24.m24bikestats.domain.model.BoschActivityPage
import info.meuse24.m24bikestats.domain.model.BoschBike
import info.meuse24.m24bikestats.domain.model.CsvSeparator
import info.meuse24.m24bikestats.domain.usecase.ExportSmartSystemActivityDetailsCsvUseCase
import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository
import info.meuse24.m24bikestats.domain.usecase.ExportSmartSystemActivitiesCsvUseCase
import info.meuse24.m24bikestats.domain.usecase.FakeAppSettingsRepository
import info.meuse24.m24bikestats.domain.usecase.GetCachedSmartSystemActivityDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.GetCachedSmartSystemActivityUseCase
import info.meuse24.m24bikestats.domain.usecase.GetCachedSmartSystemBikeUseCase
import info.meuse24.m24bikestats.domain.usecase.GetSmartSystemActivitiesUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemActivitiesUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemActivityDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemBikeDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemBikesUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveAppSettingsUseCase
import info.meuse24.m24bikestats.domain.usecase.RefreshSmartSystemActivitiesUseCase
import info.meuse24.m24bikestats.domain.usecase.RefreshSmartSystemActivityDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.RefreshSmartSystemBikeDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.RefreshSmartSystemBikesUseCase
import info.meuse24.m24bikestats.domain.usecase.SyncSmartSystemCloudUseCase
import info.meuse24.m24bikestats.domain.usecase.UpdateCsvSeparatorUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `search query filters visible activities in ui state`() = runTest {
        val repository = DashboardFakeRepository().apply {
            setActivities(
                listOf(
                    testActivity(id = "a1", title = "Morgenrunde", startTime = "2026-04-02T08:00:00Z"),
                    testActivity(id = "a2", title = "Pendelfahrt", startTime = "2026-04-01T08:00:00Z"),
                ),
                totalCount = 2,
            )
            setBikes(emptyList())
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.updateActivitySearchQuery("morgen")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("morgen", state.activitySearchQuery)
        assertEquals(1, state.visibleActivityCount)
        assertEquals(listOf("a1"), state.activities.map { it.id })
    }

    @Test
    fun `sort option reorders visible activities`() = runTest {
        val repository = DashboardFakeRepository().apply {
            setActivities(
                listOf(
                    testActivity(id = "short", title = "Kurz", distanceMeters = 1000),
                    testActivity(id = "long", title = "Lang", distanceMeters = 9000),
                    testActivity(id = "mid", title = "Mittel", distanceMeters = 4000),
                ),
                totalCount = 3,
            )
            setBikes(emptyList())
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.updateActivitySortOption(ActivitySortOption.LONGEST_DISTANCE)
        advanceUntilIdle()

        assertEquals(listOf("long", "mid", "short"), viewModel.uiState.value.activities.map { it.id })
    }

    @Test
    fun `csv export stores last export summary`() = runTest {
        val repository = DashboardFakeRepository().apply {
            setActivities(
                listOf(testActivity(id = "a1", title = "Tour")),
                totalCount = 1,
            )
            setBikes(emptyList())
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.exportAllActivitiesCsv()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.pendingActivitiesCsvExport)
        assertNotNull(state.lastActivitiesCsvExport)
        assertEquals(1, state.lastActivitiesCsvExport?.activityCount)
        assertTrue(state.lastActivitiesCsvExport?.fileName?.endsWith(".csv") == true)
    }

    @Test
    fun `detail csv export stores visible activity export summary`() = runTest {
        val repository = DashboardFakeRepository().apply {
            setActivities(
                listOf(
                    testActivity(id = "a1", title = "Morgenrunde"),
                    testActivity(id = "a2", title = "Abendrunde"),
                ),
                totalCount = 2,
            )
            setBikes(emptyList())
            setActivityDetail(
                "a1",
                BoschActivityDetail(
                    activityId = "a1",
                    points = listOf(
                        info.meuse24.m24bikestats.domain.model.BoschActivityDetailPoint(
                            distanceMeters = 100.0,
                            altitudeMeters = 500.0,
                            speedKmh = 25.0,
                            cadenceRpm = 80.0,
                            latitude = 47.1,
                            longitude = 9.1,
                            riderPowerWatts = 210.0,
                        )
                    )
                )
            )
            setActivityDetail(
                "a2",
                BoschActivityDetail(
                    activityId = "a2",
                    points = listOf(
                        info.meuse24.m24bikestats.domain.model.BoschActivityDetailPoint(
                            distanceMeters = 120.0,
                            altitudeMeters = 520.0,
                            speedKmh = 22.0,
                            cadenceRpm = 78.0,
                            latitude = 47.2,
                            longitude = 9.2,
                            riderPowerWatts = 190.0,
                        )
                    )
                )
            )
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.exportVisibleActivityDetailsCsv()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.pendingActivityDetailsCsvExport)
        assertNotNull(state.lastActivityDetailsCsvExport)
        assertEquals(2, state.lastActivityDetailsCsvExport?.activityCount)
        assertEquals(2, state.lastActivityDetailsCsvExport?.detailPointCount)
    }

    @Test
    fun `csv separator changes propagate into ui state`() = runTest {
        val repository = DashboardFakeRepository().apply {
            setActivities(emptyList(), totalCount = 0)
            setBikes(emptyList())
        }
        val settingsRepository = FakeAppSettingsRepository(CsvSeparator.COMMA)
        val viewModel = createViewModel(repository, settingsRepository)
        advanceUntilIdle()

        viewModel.updateCsvSeparator(CsvSeparator.SEMICOLON)
        advanceUntilIdle()

        assertEquals(CsvSeparator.SEMICOLON, viewModel.uiState.value.csvSeparator)
    }

    @Test
    fun `cloud sync stores last sync summary`() = runTest {
        val repository = DashboardFakeRepository().apply {
            setActivities(
                listOf(
                    testActivity(id = "a1", title = "Morgenrunde"),
                    testActivity(id = "a2", title = "Abendrunde"),
                ),
                totalCount = 2,
            )
            setBikes(
                listOf(
                    info.meuse24.m24bikestats.domain.model.BoschBike(
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
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.syncCloudData()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.lastCloudSyncSummary)
        assertEquals(2, state.lastCloudSyncSummary?.activityCount)
        assertEquals(1, state.lastCloudSyncSummary?.bikeCount)
        assertEquals(false, state.isSyncingCloudData)
    }

    private fun createViewModel(
        repository: DashboardFakeRepository,
        settingsRepository: FakeAppSettingsRepository = FakeAppSettingsRepository(),
    ): DashboardViewModel {
        val authRepository = object : AuthRepository {
            override fun getAccessToken(): String? = "token"
            override suspend fun getValidAccessToken(): Result<String> = Result.success("token")
            override fun isAuthenticated(): Boolean = true
            override fun clearTokens() = Unit
        }

        return DashboardViewModel(
            observeCachedActivities = ObserveCachedSmartSystemActivitiesUseCase(repository),
            observeCachedActivityDetail = ObserveCachedSmartSystemActivityDetailUseCase(repository),
            observeCachedBikes = ObserveCachedSmartSystemBikesUseCase(repository),
            observeCachedBikeDetail = ObserveCachedSmartSystemBikeDetailUseCase(repository),
            getCachedActivity = GetCachedSmartSystemActivityUseCase(repository),
            getCachedActivityDetail = GetCachedSmartSystemActivityDetailUseCase(repository),
            getCachedBike = GetCachedSmartSystemBikeUseCase(repository),
            getActivities = GetSmartSystemActivitiesUseCase(repository, authRepository),
            refreshActivitiesUseCase = RefreshSmartSystemActivitiesUseCase(repository, authRepository),
            exportActivitiesCsv = ExportSmartSystemActivitiesCsvUseCase(repository, authRepository, settingsRepository),
            exportActivityDetailsCsv = ExportSmartSystemActivityDetailsCsvUseCase(repository, authRepository, settingsRepository),
            refreshActivityDetailUseCase = RefreshSmartSystemActivityDetailUseCase(repository, authRepository),
            refreshBikesUseCase = RefreshSmartSystemBikesUseCase(repository, authRepository),
            refreshBikeDetailUseCase = RefreshSmartSystemBikeDetailUseCase(repository, authRepository),
            syncSmartSystemCloudUseCase = SyncSmartSystemCloudUseCase(repository, authRepository),
            observeAppSettings = ObserveAppSettingsUseCase(settingsRepository),
            updateCsvSeparatorUseCase = UpdateCsvSeparatorUseCase(settingsRepository),
            stringResolver = TestStringResolver(),
        )
    }

    private fun testActivity(
        id: String,
        title: String,
        startTime: String = "2026-04-03T10:00:00Z",
        distanceMeters: Int = 1234,
    ) = BoschActivity(
        id = id,
        title = title,
        startTime = startTime,
        endTime = null,
        timeZone = null,
        durationWithoutStopsSeconds = 1200,
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

private class TestStringResolver : DashboardStringResolver {
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

private class DashboardFakeRepository : BoschSmartSystemRepository {
    private val activitiesFlow = MutableStateFlow<List<BoschActivity>>(emptyList())
    private val bikesFlow = MutableStateFlow<List<BoschBike>>(emptyList())
    private val activityDetails = mutableMapOf<String, MutableStateFlow<BoschActivityDetail?>>()
    private val bikeDetails = mutableMapOf<String, MutableStateFlow<BoschBike?>>()
    private var activityTotalCount: Int? = null

    fun setActivities(activities: List<BoschActivity>, totalCount: Int) {
        activitiesFlow.value = activities
        activityTotalCount = totalCount
    }

    fun setBikes(bikes: List<BoschBike>) {
        bikesFlow.value = bikes
    }

    fun setActivityDetail(activityId: String, detail: BoschActivityDetail) {
        activityDetails.getOrPut(activityId) { MutableStateFlow(null) }.value = detail
    }

    override fun observeCachedActivities(): Flow<List<BoschActivity>> = activitiesFlow.asStateFlow()
    override fun observeCachedBikes(): Flow<List<BoschBike>> = bikesFlow.asStateFlow()
    override fun observeCachedActivityDetail(activityId: String): Flow<BoschActivityDetail?> =
        activityDetails.getOrPut(activityId) { MutableStateFlow(null) }.asStateFlow()

    override fun observeCachedBike(bikeId: String): Flow<BoschBike?> =
        bikeDetails.getOrPut(bikeId) { MutableStateFlow(null) }.asStateFlow()

    override suspend fun getCachedActivities(): List<BoschActivity> = activitiesFlow.value
    override suspend fun getCachedActivityTotalCount(): Int? = activityTotalCount
    override suspend fun getCachedActivity(activityId: String): BoschActivity? =
        activitiesFlow.value.firstOrNull { it.id == activityId }

    override suspend fun getCachedActivityDetail(activityId: String): BoschActivityDetail? =
        activityDetails[activityId]?.value

    override suspend fun getCachedBike(bikeId: String): BoschBike? =
        bikeDetails[bikeId]?.value ?: bikesFlow.value.firstOrNull { it.id == bikeId }

    override suspend fun isActivitiesCacheFresh(maxAgeMillis: Long): Boolean = true
    override suspend fun isActivityDetailCacheFresh(activityId: String, maxAgeMillis: Long): Boolean =
        activityDetails[activityId]?.value != null
    override suspend fun isBikesCacheFresh(maxAgeMillis: Long): Boolean = true
    override suspend fun isBikeDetailCacheFresh(bikeId: String, maxAgeMillis: Long): Boolean = true

    override suspend fun getActivities(accessToken: String, limit: Int, offset: Int): Result<BoschActivityPage> =
        Result.success(
            BoschActivityPage(
                total = activityTotalCount ?: activitiesFlow.value.size,
                offset = offset,
                limit = limit,
                items = activitiesFlow.value.drop(offset).take(limit),
            )
        )

    override suspend fun getActivityDetail(accessToken: String, activityId: String): Result<BoschActivityDetail> =
        activityDetails[activityId]?.value?.let { Result.success(it) }
            ?: Result.failure(IllegalStateException("not needed"))

    override suspend fun getBikes(accessToken: String): Result<List<BoschBike>> =
        Result.success(bikesFlow.value)

    override suspend fun getBikeDetail(accessToken: String, bikeId: String): Result<BoschBike> =
        Result.failure(IllegalStateException("not needed"))
}
