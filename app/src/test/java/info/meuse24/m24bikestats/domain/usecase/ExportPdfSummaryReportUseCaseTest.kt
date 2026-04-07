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
                    averageSpeedKmh = 23.4,
                    maxSpeedKmh = 38.2,
                    maxRiderPowerWatts = 412.0,
                    elevationGainMeters = 640,
                    caloriesBurned = 820.0,
                    centerLatitude = 48.2082,
                    centerLongitude = 16.3738,
                ),
                activity(
                    id = "a2",
                    title = "Evening",
                    startTime = "2026-04-02T18:00:00Z",
                    distanceMeters = 20000,
                    durationWithoutStopsSeconds = 3600,
                    averageSpeedKmh = 19.8,
                    maxSpeedKmh = 32.8,
                    maxRiderPowerWatts = 290.0,
                    elevationGainMeters = 280,
                    caloriesBurned = 410.0,
                    centerLatitude = 47.0707,
                    centerLongitude = 15.4395,
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
        assertEquals(1, report.statistics.weeklyPeriods.size)
        assertEquals(1, report.statistics.monthlyPeriods.size)
        assertEquals(1, report.statistics.yearlyPeriods.size)
        assertEquals(java.time.DayOfWeek.WEDNESDAY, report.statistics.highlights.favoriteDayOfWeek)
        assertEquals(2, report.statistics.dayOfWeekDistribution.size)
        assertEquals(1.5, report.statistics.highlights.longestRideHours, 0.001)
        assertEquals(23.4, report.statistics.highlights.fastestTourAvgSpeedKmh ?: 0.0, 0.001)
        assertEquals("KW 14", report.statistics.strongestWeek?.label)
        assertEquals("Apr 26", report.statistics.strongestMonth?.label)
        assertEquals("2026", report.statistics.strongestYear?.label)
        assertEquals(38.2, report.statistics.highlights.maxSpeedKmh ?: 0.0, 0.001)
        assertEquals(412.0, report.statistics.highlights.maxRiderPowerWatts ?: 0.0, 0.001)
        assertNull(report.statistics.activeWeeksRatio)
        assertEquals(listOf(48.2082 to 16.3738, 47.0707 to 15.4395), report.mapPoints)
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

    @Test
    fun `strongest periods pick the highest distance across multiple buckets`() = runTest {
        val useCase = ExportPdfSummaryReportUseCase(
            metadataRepository = FakePdfMetadataRepository(),
            repository = FakePdfRepository(
                activities = listOf(
                    activity(
                        id = "w1",
                        title = "Week 1",
                        startTime = "2026-01-05T08:00:00Z",
                        distanceMeters = 15000,
                        durationWithoutStopsSeconds = 3600,
                    ),
                    activity(
                        id = "w2",
                        title = "Week 2",
                        startTime = "2026-01-13T08:00:00Z",
                        distanceMeters = 32000,
                        durationWithoutStopsSeconds = 5400,
                    ),
                    activity(
                        id = "m2",
                        title = "Month 2",
                        startTime = "2026-02-10T08:00:00Z",
                        distanceMeters = 28000,
                        durationWithoutStopsSeconds = 4000,
                    ),
                    activity(
                        id = "y2",
                        title = "Year 2",
                        startTime = "2027-03-10T08:00:00Z",
                        distanceMeters = 18000,
                        durationWithoutStopsSeconds = 2700,
                    ),
                ),
            ),
            localeProvider = { Locale.GERMANY },
            zoneIdProvider = { ZoneId.of("Europe/Vienna") },
        )

        val report = useCase().getOrThrow()

        assertEquals(4, report.statistics.weeklyPeriods.size)
        assertEquals(3, report.statistics.monthlyPeriods.size)
        assertEquals(2, report.statistics.yearlyPeriods.size)
        assertEquals("KW 3", report.statistics.strongestWeek?.label)
        assertEquals(32.0, report.statistics.strongestWeek?.distanceKm ?: 0.0, 0.001)
        assertEquals("Jan 26", report.statistics.strongestMonth?.label)
        assertEquals(47.0, report.statistics.strongestMonth?.distanceKm ?: 0.0, 0.001)
        assertEquals("2026", report.statistics.strongestYear?.label)
        assertEquals(75.0, report.statistics.strongestYear?.distanceKm ?: 0.0, 0.001)
    }

    @Test
    fun `empty activity list keeps statistics aggregates empty`() = runTest {
        val useCase = ExportPdfSummaryReportUseCase(
            metadataRepository = FakePdfMetadataRepository(),
            repository = FakePdfRepository(),
        )

        val report = useCase().getOrThrow()

        assertEquals(0, report.activitySummary.totalTours)
        assertEquals(0.0, report.statistics.highlights.longestTourKm, 0.0)
        assertEquals(0.0, report.statistics.highlights.longestRideHours, 0.0)
        assertNull(report.statistics.highlights.favoriteDayOfWeek)
        assertTrue(report.statistics.weeklyPeriods.isEmpty())
        assertTrue(report.statistics.monthlyPeriods.isEmpty())
        assertTrue(report.statistics.yearlyPeriods.isEmpty())
        assertNull(report.statistics.strongestWeek)
        assertNull(report.statistics.strongestMonth)
        assertNull(report.statistics.strongestYear)
    }

    private fun activity(
        id: String,
        title: String,
        startTime: String = "2026-04-01T08:00:00Z",
        distanceMeters: Int = 10000,
        durationWithoutStopsSeconds: Int = 1800,
        averageSpeedKmh: Double? = null,
        maxSpeedKmh: Double? = null,
        maxRiderPowerWatts: Double? = null,
        elevationGainMeters: Int? = null,
        caloriesBurned: Double? = null,
        centerLatitude: Double? = null,
        centerLongitude: Double? = null,
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
        averageSpeedKmh = averageSpeedKmh,
        maxSpeedKmh = maxSpeedKmh,
        averageCadenceRpm = null,
        maxCadenceRpm = null,
        averageRiderPowerWatts = null,
        maxRiderPowerWatts = maxRiderPowerWatts,
        elevationGainMeters = elevationGainMeters,
        elevationLossMeters = null,
        caloriesBurned = caloriesBurned,
        centerLatitude = centerLatitude,
        centerLongitude = centerLongitude,
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
