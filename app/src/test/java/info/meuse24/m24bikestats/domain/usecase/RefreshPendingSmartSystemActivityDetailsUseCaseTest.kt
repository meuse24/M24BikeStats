package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.model.BoschActivityDetail
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RefreshPendingSmartSystemActivityDetailsUseCaseTest {

    @Test
    fun `refresh missing only loads only missing activities`() = runTest {
        val repository = FakeBoschSmartSystemRepository().apply {
            cachedActivities = listOf(
                activity("a1"),
                activity("a2"),
                activity("a3"),
            )
            cachedActivityDetails["a2"] = BoschActivityDetail(activityId = "a2", points = emptyList())
            activityDetailResult = Result.success(BoschActivityDetail(activityId = "unused", points = emptyList()))
            activityDetailResultsById["a1"] = Result.success(BoschActivityDetail(activityId = "a1", points = emptyList()))
            activityDetailResultsById["a3"] = Result.success(BoschActivityDetail(activityId = "a3", points = emptyList()))
        }

        val refreshedCount = RefreshPendingSmartSystemActivityDetailsUseCase(
            repository = repository,
            cacheStatusRepository = repository,
            authRepository = FakeAuthRepository(),
        ).refreshMissing().getOrThrow()

        assertEquals(2, refreshedCount)
        assertEquals(listOf("a1", "a3"), repository.getActivityDetailCalls)
    }

    @Test
    fun `refresh stale only skips missing details`() = runTest {
        val repository = FakeBoschSmartSystemRepository().apply {
            cachedActivities = listOf(
                activity("a1"),
                activity("a2"),
                activity("a3"),
            )
            cachedActivityDetails["a1"] = BoschActivityDetail(activityId = "a1", points = emptyList())
            staleActivityIds += "a1"
            activityDetailResultsById["a1"] = Result.success(BoschActivityDetail(activityId = "a1", points = emptyList()))
        }

        val refreshedCount = RefreshPendingSmartSystemActivityDetailsUseCase(
            repository = repository,
            cacheStatusRepository = repository,
            authRepository = FakeAuthRepository(),
        ).refreshStale().getOrThrow()

        assertEquals(1, refreshedCount)
        assertEquals(listOf("a1"), repository.getActivityDetailCalls)
    }

    private fun activity(id: String) = BoschActivity(
        id = id,
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
}
