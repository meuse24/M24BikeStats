package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.model.BoschActivityDetail
import info.meuse24.m24bikestats.domain.model.BoschActivityPage
import info.meuse24.m24bikestats.domain.model.BoschBike
import info.meuse24.m24bikestats.domain.model.PdfReportDiscoveryInfo
import info.meuse24.m24bikestats.domain.model.PdfReportUserInfo
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository
import info.meuse24.m24bikestats.domain.repository.PdfReportMetadataRepository
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportPdfSummaryReportUseCaseTest {

    @Test
    fun `builds aggregated pdf report data from cached repositories`() = runTest {
        val repository = FakePdfRepository(
            bikes = listOf(
                BoschBike(
                    id = "bike-1",
                    createdAt = null,
                    language = "de",
                    driveUnit = null,
                    remoteControl = null,
                    headUnit = null,
                    batteries = emptyList(),
                )
            ),
            activities = listOf(
                activity(
                    id = "a1",
                    title = "Morning",
                    startTime = "2026-03-31T22:30:00Z",
                    distanceMeters = 32000,
                    durationWithoutStopsSeconds = 5400,
                    maxSpeedKmh = 38.2,
                    maxRiderPowerWatts = 412.0,
                    elevationGainMeters = 640,
                    caloriesBurned = 820.0,
                ),
                activity(
                    id = "a2",
                    title = "Evening",
                    startTime = "2026-04-02T18:00:00Z",
                    distanceMeters = 20000,
                    durationWithoutStopsSeconds = 3600,
                    maxSpeedKmh = 32.8,
                    maxRiderPowerWatts = 290.0,
                    elevationGainMeters = 280,
                    caloriesBurned = 410.0,
                ),
            ),
        )
        val useCase = ExportPdfSummaryReportUseCase(
            metadataRepository = FakePdfMetadataRepository(),
            repository = repository,
            localeProvider = { Locale.GERMANY },
            zoneIdProvider = { ZoneId.of("Europe/Vienna") },
            clock = { Instant.parse("2026-04-06T08:30:00Z") },
        )

        val result = useCase()

        assertTrue(result.isSuccess)
        val report = result.getOrThrow()
        assertEquals(2, report.activitySummary.totalTours)
        assertEquals(52.0, report.activitySummary.totalDistanceKm, 0.001)
        assertEquals(2.5, report.activitySummary.totalDurationHours, 0.001)
        assertEquals(20.8, report.activitySummary.avgTravelSpeedKmh ?: 0.0, 0.001)
        assertEquals(920, report.activitySummary.totalElevationGainM)
        assertEquals(1230.0, report.activitySummary.totalCaloriesBurned ?: 0.0, 0.001)
        assertEquals(1, report.bikes.size)
        assertEquals("test@example.com", report.userInfo?.email)
        assertEquals("https://issuer.example", report.discoveryInfo?.issuer)
        assertEquals(1, report.statistics.monthlyPeriods.size)
        assertEquals(java.time.DayOfWeek.WEDNESDAY, report.statistics.highlights.favoriteDayOfWeek)
        assertEquals(2, report.statistics.dayOfWeekDistribution.size)
        assertEquals(38.2, report.statistics.highlights.maxSpeedKmh ?: 0.0, 0.001)
        assertEquals(412.0, report.statistics.highlights.maxRiderPowerWatts ?: 0.0, 0.001)
        assertNull(report.statistics.activeWeeksRatio)
    }

    @Test
    fun `propagates metadata failures`() = runTest {
        val useCase = ExportPdfSummaryReportUseCase(
            metadataRepository = object : PdfReportMetadataRepository {
                override suspend fun getCurrentUserInfo(): Result<PdfReportUserInfo?> =
                    Result.failure(IllegalStateException("userinfo failed"))

                override suspend fun getCurrentDiscoveryInfo(): Result<PdfReportDiscoveryInfo?> =
                    Result.success(null)
            },
            repository = FakePdfRepository(),
        )

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals("userinfo failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun `keeps calories nullable when no activity has calorie data`() = runTest {
        val useCase = ExportPdfSummaryReportUseCase(
            metadataRepository = FakePdfMetadataRepository(),
            repository = FakePdfRepository(
                activities = listOf(
                    activity(
                        id = "a1",
                        title = "Tour",
                        caloriesBurned = null,
                    )
                ),
            ),
        )

        val report = useCase().getOrThrow()

        assertNull(report.activitySummary.totalCaloriesBurned)
    }

    private fun activity(
        id: String,
        title: String,
        startTime: String = "2026-04-01T08:00:00Z",
        distanceMeters: Int = 10000,
        durationWithoutStopsSeconds: Int = 1800,
        maxSpeedKmh: Double? = null,
        maxRiderPowerWatts: Double? = null,
        elevationGainMeters: Int? = null,
        caloriesBurned: Double? = null,
    ) = BoschActivity(
        id = id,
        title = title,
        startTime = startTime,
        endTime = null,
        timeZone = null,
        durationWithoutStopsSeconds = durationWithoutStopsSeconds,
        bikeId = null,
        startOdometerMeters = null,
        distanceMeters = distanceMeters,
        averageSpeedKmh = null,
        maxSpeedKmh = maxSpeedKmh,
        averageCadenceRpm = null,
        maxCadenceRpm = null,
        averageRiderPowerWatts = null,
        maxRiderPowerWatts = maxRiderPowerWatts,
        elevationGainMeters = elevationGainMeters,
        elevationLossMeters = null,
        caloriesBurned = caloriesBurned,
    )
}

private class FakePdfMetadataRepository : PdfReportMetadataRepository {
    override suspend fun getCurrentUserInfo(): Result<PdfReportUserInfo?> =
        Result.success(
            PdfReportUserInfo(
                email = "test@example.com",
                username = "tester",
                subject = "sub-1",
            )
        )

    override suspend fun getCurrentDiscoveryInfo(): Result<PdfReportDiscoveryInfo?> =
        Result.success(
            PdfReportDiscoveryInfo(
                issuer = "https://issuer.example",
                authorizationEndpoint = "https://issuer.example/auth",
                tokenEndpoint = "https://issuer.example/token",
                userInfoEndpoint = "https://issuer.example/userinfo",
            )
        )
}

private class FakePdfRepository(
    bikes: List<BoschBike> = emptyList(),
    private val activities: List<BoschActivity> = emptyList(),
) : BoschSmartSystemRepository {
    private val bikesFlow = MutableStateFlow(bikes)

    override fun observeCachedActivities(): Flow<List<BoschActivity>> =
        MutableStateFlow(activities).asStateFlow()

    override fun observeCachedBikes(): Flow<List<BoschBike>> = bikesFlow.asStateFlow()

    override fun observeCachedActivityDetail(activityId: String): Flow<BoschActivityDetail?> =
        MutableStateFlow<BoschActivityDetail?>(null).asStateFlow()

    override fun observeCachedBike(bikeId: String): Flow<BoschBike?> =
        MutableStateFlow<BoschBike?>(bikesFlow.value.firstOrNull { it.id == bikeId }).asStateFlow()

    override suspend fun getCachedActivities(): List<BoschActivity> = activities

    override suspend fun getCachedActivity(activityId: String): BoschActivity? =
        activities.firstOrNull { it.id == activityId }

    override suspend fun getCachedActivityDetail(activityId: String): BoschActivityDetail? = null

    override suspend fun getCachedBike(bikeId: String): BoschBike? =
        bikesFlow.value.firstOrNull { it.id == bikeId }

    override suspend fun getActivities(accessToken: String, limit: Int, offset: Int): Result<BoschActivityPage> =
        Result.failure(UnsupportedOperationException())

    override suspend fun getActivityDetail(accessToken: String, activityId: String): Result<BoschActivityDetail> =
        Result.failure(UnsupportedOperationException())

    override suspend fun getBikes(accessToken: String): Result<List<BoschBike>> =
        Result.failure(UnsupportedOperationException())

    override suspend fun getBikeDetail(accessToken: String, bikeId: String): Result<BoschBike> =
        Result.failure(UnsupportedOperationException())
}
