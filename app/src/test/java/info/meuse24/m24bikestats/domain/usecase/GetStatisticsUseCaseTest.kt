package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.model.BoschActivityDetail
import info.meuse24.m24bikestats.domain.model.BoschActivityPage
import info.meuse24.m24bikestats.domain.model.BoschBike
import info.meuse24.m24bikestats.domain.model.StatisticsGrouping
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetStatisticsUseCaseTest {

    @Test
    fun `groups activities by week and sums metrics`() = runTest {
        val overview = createUseCase(
            listOf(
                activity("2026-04-01T08:00:00Z", distanceMeters = 10000, durationSeconds = 1800),
                activity("2026-04-03T09:00:00Z", distanceMeters = 25000, durationSeconds = 3600),
                activity("2026-04-10T09:00:00Z", distanceMeters = 5000, durationSeconds = 900),
            ),
        ).invoke(StatisticsGrouping.WEEK).first()

        assertEquals(2, overview.periods.size)
        assertEquals(2, overview.periods[0].tourCount)
        assertEquals(35.0, overview.periods[0].distanceKm, 0.0)
        assertEquals(90, overview.periods[0].durationMinutes)
        assertEquals(3, overview.totalTours)
        assertEquals(40.0, overview.totalDistanceKm, 0.0)
    }

    @Test
    fun `groups activities by month using local timezone`() = runTest {
        val overview = createUseCase(
            listOf(
                activity("2026-03-31T22:30:00Z", distanceMeters = 10000, durationSeconds = 1800),
                activity("2026-04-15T09:00:00Z", distanceMeters = 5000, durationSeconds = 900),
            ),
        ).invoke(StatisticsGrouping.MONTH).first()

        assertEquals(1, overview.periods.size)
        assertEquals(2, overview.periods[0].tourCount)
        assertEquals(15.0, overview.periods[0].distanceKm, 0.0)
        assertEquals(
            LocalDate.of(2026, 4, 1).atStartOfDay(ZoneId.of("Europe/Vienna")).toInstant().toEpochMilli(),
            overview.coveredPeriodStartEpochMillis,
        )
        assertEquals(
            LocalDate.of(2026, 4, 15).atStartOfDay(ZoneId.of("Europe/Vienna")).toInstant().toEpochMilli(),
            overview.coveredPeriodEndEpochMillis,
        )
    }

    @Test
    fun `highlights compute favorite day and fill weekly histogram gaps`() = runTest {
        val highlights = createUseCase(
            listOf(
                activity("2026-04-04T08:00:00Z", distanceMeters = 10000, durationSeconds = 1800),
                activity("2026-04-18T08:00:00Z", distanceMeters = 11000, durationSeconds = 1900),
                activity("2026-04-20T08:00:00Z", distanceMeters = 12000, durationSeconds = 2000),
            ),
        ).invoke(StatisticsGrouping.MONTH).first().highlights

        requireNotNull(highlights)
        assertEquals(DayOfWeek.SATURDAY, highlights.favoriteDayOfWeek)
        assertEquals(2, highlights.dayOfWeekDistribution[DayOfWeek.SATURDAY])
        assertEquals(mapOf(0 to 1, 1 to 3), highlights.weeklyFrequencyHistogram)
        assertEquals(3.0 / 4.0, highlights.activeWeeksRatio ?: 0.0, 0.0)
    }

    @Test
    fun `most active period breaks ties by higher tour count`() = runTest {
        val overview = createUseCase(
            listOf(
                activity("2026-04-01T08:00:00Z", distanceMeters = 12000, durationSeconds = 1800),
                activity("2026-04-03T08:00:00Z", distanceMeters = 8000, durationSeconds = 1200),
                activity("2026-04-08T08:00:00Z", distanceMeters = 20000, durationSeconds = 2100),
                activity("2026-04-15T08:00:00Z", distanceMeters = 20000, durationSeconds = 2100),
            ),
        ).invoke(StatisticsGrouping.WEEK).first()

        val highlights = requireNotNull(overview.highlights)
        val mostActive = overview.periods.first { it.startEpochMillis == highlights.mostActivePeriodStartEpochMillis }
        assertEquals(20.0, mostActive.distanceKm, 0.0)
        assertEquals(2, overview.periods[0].tourCount)
        assertEquals(overview.periods[0].startEpochMillis, highlights.mostActivePeriodStartEpochMillis)
    }

    @Test
    fun `keeps nullable metrics null and average travel speed null when duration is zero`() = runTest {
        val highlights = createUseCase(
            listOf(
                activity("2026-04-01T08:00:00Z", distanceMeters = 10000, durationSeconds = 0),
            ),
        ).invoke(StatisticsGrouping.MONTH).first().highlights

        requireNotNull(highlights)
        assertNull(highlights.maxSpeedKmh)
        assertNull(highlights.fastestTourAvgSpeedKmh)
        assertNull(highlights.maxRiderPowerWatts)
        assertNull(highlights.totalCaloriesBurned)
        assertNull(highlights.avgTravelSpeedKmh)
    }

    private fun createUseCase(
        activities: List<BoschActivity>,
    ): GetStatisticsUseCase =
        GetStatisticsUseCase(
            repository = StatisticsRepository(activities),
            zoneIdProvider = { ZoneId.of("Europe/Vienna") },
            localeProvider = { Locale.GERMAN },
        )

    private fun activity(
        startTime: String,
        distanceMeters: Int,
        durationSeconds: Int,
        averageSpeedKmh: Double? = null,
        maxSpeedKmh: Double? = null,
        maxRiderPowerWatts: Double? = null,
        caloriesBurned: Double? = null,
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
        maxSpeedKmh = maxSpeedKmh,
        averageCadenceRpm = null,
        maxCadenceRpm = null,
        averageRiderPowerWatts = null,
        maxRiderPowerWatts = maxRiderPowerWatts,
        elevationGainMeters = null,
        elevationLossMeters = null,
        caloriesBurned = caloriesBurned,
    )
}

private class StatisticsRepository(
    activities: List<BoschActivity>,
) : BoschSmartSystemRepository {
    private val activitiesFlow = MutableStateFlow(activities)

    override fun observeCachedActivities(): Flow<List<BoschActivity>> = activitiesFlow.asStateFlow()
    override fun observeCachedBikes(): Flow<List<BoschBike>> = MutableStateFlow(emptyList<BoschBike>()).asStateFlow()
    override fun observeCachedActivityDetail(activityId: String): Flow<BoschActivityDetail?> = MutableStateFlow(null).asStateFlow()
    override fun observeCachedBike(bikeId: String): Flow<BoschBike?> = MutableStateFlow(null).asStateFlow()
    override suspend fun getCachedActivities(): List<BoschActivity> = activitiesFlow.value
    override suspend fun getCachedActivity(activityId: String): BoschActivity? = activitiesFlow.value.firstOrNull { it.id == activityId }
    override suspend fun getCachedActivityDetail(activityId: String): BoschActivityDetail? = null
    override suspend fun getCachedBike(bikeId: String): BoschBike? = null
    override suspend fun getActivities(accessToken: String, limit: Int, offset: Int): Result<BoschActivityPage> = error("not used")
    override suspend fun getActivityDetail(accessToken: String, activityId: String): Result<BoschActivityDetail> = error("not used")
    override suspend fun getBikes(accessToken: String): Result<List<BoschBike>> = error("not used")
    override suspend fun getBikeDetail(accessToken: String, bikeId: String): Result<BoschBike> = error("not used")
}
