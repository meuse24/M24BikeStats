package info.meuse24.m24bikestats.presentation.statistics

import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.model.BoschActivityDetail
import info.meuse24.m24bikestats.domain.model.BoschActivityPage
import info.meuse24.m24bikestats.domain.model.BoschBike
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository
import info.meuse24.m24bikestats.domain.usecase.GetStatisticsUseCase
import info.meuse24.m24bikestats.presentation.dashboard.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneId
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `toggle selected period deselects on second tap`() = runTest {
        val viewModel = createViewModel(
            listOf(
                activity("2026-04-01T08:00:00Z"),
                activity("2026-04-03T09:00:00Z"),
            ),
        )
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val selectedStart = viewModel.uiState.value.periods.first().startEpochMillis

        viewModel.toggleSelectedPeriod(selectedStart)
        advanceUntilIdle()
        assertEquals(selectedStart, viewModel.uiState.value.selectedPeriod?.startEpochMillis)

        viewModel.toggleSelectedPeriod(selectedStart)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.selectedPeriod)
        collector.cancel()
    }

    @Test
    fun `update grouping clears selected period`() = runTest {
        val viewModel = createViewModel(
            listOf(
                activity("2026-04-01T08:00:00Z"),
                activity("2026-04-15T09:00:00Z"),
            ),
        )
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val selectedStart = viewModel.uiState.value.periods.first().startEpochMillis
        viewModel.toggleSelectedPeriod(selectedStart)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.selectedPeriod)

        viewModel.updateGrouping(StatisticsGrouping.WEEK)
        advanceUntilIdle()

        assertEquals(StatisticsGrouping.WEEK, viewModel.uiState.value.grouping)
        assertNull(viewModel.uiState.value.selectedPeriod)
        collector.cancel()
    }

    @Test
    fun `grouping change does not retain stale selected period reference`() = runTest {
        val viewModel = createViewModel(
            listOf(
                activity("2026-04-05T08:00:00Z"),
                activity("2026-04-12T09:00:00Z"),
            ),
        )
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.updateGrouping(StatisticsGrouping.WEEK)
        advanceUntilIdle()
        val selectedStart = viewModel.uiState.value.periods.first().startEpochMillis
        viewModel.toggleSelectedPeriod(selectedStart)
        advanceUntilIdle()
        assertEquals(selectedStart, viewModel.uiState.value.selectedPeriod?.startEpochMillis)

        viewModel.updateGrouping(StatisticsGrouping.MONTH)
        advanceUntilIdle()

        assertEquals(StatisticsGrouping.MONTH, viewModel.uiState.value.grouping)
        assertNull(viewModel.uiState.value.selectedPeriod)
        collector.cancel()
    }

    @Test
    fun `ui state exposes total and average metrics`() = runTest {
        val viewModel = createViewModel(
            listOf(
                activity("2026-04-01T08:00:00Z", distanceMeters = 10000, durationSeconds = 1800),
                activity("2026-04-03T09:00:00Z", distanceMeters = 5000, durationSeconds = 900),
            ),
        )
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        with(viewModel.uiState.value) {
            assertEquals(2, totalTours)
            assertEquals(15.0, totalDistanceKm, 0.0)
            assertEquals(0.75, totalDurationHours, 0.0)
            assertEquals(7.5, avgDistanceKm, 0.0)
            assertEquals(0.375, avgDurationHours, 0.0)
        }

        collector.cancel()
    }

    @Test
    fun `ui state exposes computed highlights`() = runTest {
        val viewModel = createViewModel(
            listOf(
                activity("2026-04-04T08:00:00Z", distanceMeters = 42000, durationSeconds = 7200, averageSpeedKmh = 24.1),
                activity("2026-04-11T08:00:00Z", distanceMeters = 10000, durationSeconds = 1800, averageSpeedKmh = 18.4),
            ),
        )
        val collector = backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val highlights = viewModel.uiState.value.highlights
        assertNotNull(highlights)
        assertEquals(42.0, highlights!!.longestTourKm, 0.0)
        assertEquals(2.0, highlights.longestRideHours, 0.0)
        assertEquals(24.1, highlights.fastestTourAvgSpeedKmh!!, 0.0)
        assertEquals(20.8, highlights.avgTravelSpeedKmh!!, 0.0)
        assertEquals("Apr 26", highlights.mostActivePeriod!!.label)
        assertEquals(1.0, highlights.activeWeeksRatio!!, 0.0)
        assertEquals(DayOfWeek.SATURDAY, highlights.favoriteDayOfWeek)
        assertEquals(2, highlights.dayOfWeekDistribution[DayOfWeek.SATURDAY])

        collector.cancel()
    }

    private fun createViewModel(
        activities: List<BoschActivity>,
    ): StatisticsViewModel {
        val repository = StatisticsFakeRepository().apply {
            setActivities(activities)
        }
        return StatisticsViewModel(
            getStatisticsUseCase = GetStatisticsUseCase(repository),
            uiModelMapper = StatisticsUiModelMapper(
                zoneId = ZoneId.of("Europe/Vienna"),
                locale = Locale.GERMAN,
            ),
        )
    }

    private fun activity(
        startTime: String,
        distanceMeters: Int = 12000,
        durationSeconds: Int = 1800,
        averageSpeedKmh: Double? = null,
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
        averageSpeedKmh = averageSpeedKmh,
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

private class StatisticsFakeRepository : BoschSmartSystemRepository {
    private val activities = MutableStateFlow<List<BoschActivity>>(emptyList())

    fun setActivities(value: List<BoschActivity>) {
        activities.value = value
    }

    override fun observeCachedActivities(): Flow<List<BoschActivity>> = activities.asStateFlow()
    override fun observeCachedBikes(): Flow<List<BoschBike>> = MutableStateFlow(emptyList<BoschBike>()).asStateFlow()
    override fun observeCachedActivityDetail(activityId: String): Flow<BoschActivityDetail?> = MutableStateFlow(null).asStateFlow()
    override fun observeCachedBike(bikeId: String): Flow<BoschBike?> = MutableStateFlow(null).asStateFlow()
    override suspend fun getCachedActivities(): List<BoschActivity> = activities.value
    override suspend fun getCachedActivity(activityId: String): BoschActivity? = activities.value.firstOrNull { it.id == activityId }
    override suspend fun getCachedActivityDetail(activityId: String): BoschActivityDetail? = null
    override suspend fun getCachedBike(bikeId: String): BoschBike? = null
    override suspend fun getActivities(accessToken: String, limit: Int, offset: Int): Result<BoschActivityPage> = error("not used")
    override suspend fun getActivityDetail(accessToken: String, activityId: String): Result<BoschActivityDetail> = error("not used")
    override suspend fun getBikes(accessToken: String): Result<List<BoschBike>> = error("not used")
    override suspend fun getBikeDetail(accessToken: String, bikeId: String): Result<BoschBike> = error("not used")
}
