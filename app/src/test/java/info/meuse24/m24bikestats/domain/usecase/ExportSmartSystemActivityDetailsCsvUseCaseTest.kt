package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.model.BoschActivityDetail
import info.meuse24.m24bikestats.domain.model.BoschActivityDetailPoint
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportSmartSystemActivityDetailsCsvUseCaseTest {

    @Test
    fun `exports cached detail rows for selected activities`() = runTest {
        val repository = FakeBoschSmartSystemRepository().apply {
            cachedActivities = listOf(
                activity(id = "a1", title = "Morgenrunde"),
                activity(id = "a2", title = "Abendrunde"),
            )
            cachedActivityDetails["a1"] = detail(
                "a1",
                BoschActivityDetailPoint(100.0, 500.0, 23.4, 80.0, 47.1, 9.1, 210.0),
            )
            cachedActivityDetails["a2"] = detail(
                "a2",
                BoschActivityDetailPoint(200.0, 505.0, 21.0, 76.0, 47.2, 9.2, 180.0),
            )
            activityDetailFresh = true
        }

        val progressEvents = mutableListOf<Pair<Int, Int>>()
        val useCase = ExportSmartSystemActivityDetailsCsvUseCase(
            repository = repository,
            authRepository = FakeAuthRepository(),
        )

        val export = useCase(listOf("a1", "a2")) { processed, total ->
            progressEvents += processed to total
        }.getOrThrow()

        assertEquals(2, export.activityCount)
        assertEquals(2, export.detailPointCount)
        assertTrue(export.csvContent.contains("\"activity_id\",\"activity_title\""))
        assertTrue(export.csvContent.contains("\"a1\",\"Morgenrunde\""))
        assertTrue(export.csvContent.contains("\"a2\",\"Abendrunde\""))
        assertTrue(repository.getActivityDetailCalls.isEmpty())
        assertEquals(listOf(1 to 2, 2 to 2), progressEvents)
    }

    @Test
    fun `falls back to remote detail when cache missing`() = runTest {
        val repository = FakeBoschSmartSystemRepository().apply {
            cachedActivities = listOf(activity(id = "a1", title = "Morgenrunde"))
            activityDetailFresh = false
            activityDetailResult = Result.success(
                detail(
                    "a1",
                    BoschActivityDetailPoint(100.0, 500.0, 23.4, 80.0, 47.1, 9.1, 210.0),
                )
            )
        }

        val export = ExportSmartSystemActivityDetailsCsvUseCase(
            repository = repository,
            authRepository = FakeAuthRepository(),
        )(listOf("a1")).getOrThrow()

        assertEquals(listOf("a1"), repository.getActivityDetailCalls)
        assertEquals(1, export.activityCount)
        assertEquals(1, export.detailPointCount)
    }

    private fun activity(id: String, title: String) = BoschActivity(
        id = id,
        title = title,
        startTime = "2026-04-03T10:00:00Z",
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

    private fun detail(activityId: String, vararg points: BoschActivityDetailPoint) =
        BoschActivityDetail(activityId = activityId, points = points.toList())
}
