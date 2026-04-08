package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.ActivityDetailCacheOverview
import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.model.BoschActivityDetail
import info.meuse24.m24bikestats.domain.model.BoschActivityPage
import info.meuse24.m24bikestats.domain.model.BoschBike
import info.meuse24.m24bikestats.domain.model.CloudSyncDetailMode
import info.meuse24.m24bikestats.domain.model.DataStatusState
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemCacheStatusRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveDataStatusOverviewUseCaseTest {

    @Test
    fun `aggregates covered period missing and stale details`() = runTest {
        val repository = DataStatusRepository().apply {
            setActivities(
                listOf(
                    activity("a1", "2026-04-01T08:00:00Z"),
                    activity("a2", "2026-04-02T08:00:00Z"),
                    activity("a3", "2026-04-03T08:00:00Z"),
                )
            )
            setDetail("a1")
            setStaleActivityIds(setOf("a1"))
            setCacheTimestamps(activityUpdatedAt = 10L, bikeUpdatedAt = 20L, detailUpdatedAt = 30L)
        }

        val overview = ObserveDataStatusOverviewUseCase(
            repository = repository,
            cacheStatusRepository = repository,
            nowMillis = { 1_000_000L },
        ).invoke().first()

        assertEquals(3, overview.cachedActivityCount)
        assertEquals(1, overview.detailedActivityCount)
        assertEquals(2, overview.missingDetailCount)
        assertEquals(1, overview.staleDetailCount)
        assertEquals(30L, overview.lastDetailSyncAtEpochMillis)
        assertEquals(DataStatusState.PARTIAL, overview.status)
    }

    @Test
    fun `returns empty state when no activities are cached`() = runTest {
        val repository = DataStatusRepository()

        val overview = ObserveDataStatusOverviewUseCase(
            repository = repository,
            cacheStatusRepository = repository,
        ).invoke().first()

        assertEquals(0, overview.cachedActivityCount)
        assertEquals(DataStatusState.EMPTY, overview.status)
    }

    @Test
    fun `returns complete state when all details exist and nothing is stale`() = runTest {
        val repository = DataStatusRepository().apply {
            setActivities(
                listOf(
                    activity("a1", "2026-04-01T08:00:00Z"),
                    activity("a2", "2026-04-02T08:00:00Z"),
                )
            )
            setDetail("a1")
            setDetail("a2")
        }

        val overview = ObserveDataStatusOverviewUseCase(
            repository = repository,
            cacheStatusRepository = repository,
        ).invoke().first()

        assertEquals(0, overview.missingDetailCount)
        assertEquals(0, overview.staleDetailCount)
        assertEquals(DataStatusState.COMPLETE, overview.status)
    }

    @Test
    fun `returns stale state when all details exist but some are stale`() = runTest {
        val repository = DataStatusRepository().apply {
            setActivities(
                listOf(
                    activity("a1", "2026-04-01T08:00:00Z"),
                    activity("a2", "2026-04-02T08:00:00Z"),
                )
            )
            setDetail("a1")
            setDetail("a2")
            setStaleActivityIds(setOf("a1"))
        }

        val overview = ObserveDataStatusOverviewUseCase(
            repository = repository,
            cacheStatusRepository = repository,
        ).invoke().first()

        assertEquals(0, overview.missingDetailCount)
        assertEquals(1, overview.staleDetailCount)
        assertEquals(DataStatusState.STALE, overview.status)
    }

    @Test
    fun `uses injected timezone for covered period boundaries`() = runTest {
        val zoneId = ZoneId.of("Europe/Vienna")
        val repository = DataStatusRepository().apply {
            setActivities(
                listOf(
                    activity("a1", "2026-03-31T22:30:00Z"),
                    activity("a2", "2026-04-15T09:00:00Z"),
                )
            )
        }

        val overview = ObserveDataStatusOverviewUseCase(
            repository = repository,
            cacheStatusRepository = repository,
            zoneIdProvider = { zoneId },
        ).invoke().first()

        assertEquals(
            LocalDate.of(2026, 4, 1).atStartOfDay(zoneId).toInstant().toEpochMilli(),
            overview.coveredActivityStartEpochMillis,
        )
        assertEquals(
            LocalDate.of(2026, 4, 15).atStartOfDay(zoneId).toInstant().toEpochMilli(),
            overview.coveredActivityEndEpochMillis,
        )
    }

    private fun activity(id: String, startTime: String) = BoschActivity(
        id = id,
        title = "Ride",
        startTime = startTime,
        endTime = null,
        timeZone = null,
        durationWithoutStopsSeconds = 1200,
        bikeId = null,
        startOdometerMeters = null,
        distanceMeters = 1200,
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

private class DataStatusRepository :
    BoschSmartSystemRepository,
    BoschSmartSystemCacheStatusRepository {
    private val activitiesFlow = MutableStateFlow<List<BoschActivity>>(emptyList())
    private val detailOverviewFlow = MutableStateFlow(ActivityDetailCacheOverview(0, 0, 0))
    private val activityCacheUpdatedAtFlow = MutableStateFlow<Long?>(null)
    private val bikeCacheUpdatedAtFlow = MutableStateFlow<Long?>(null)
    private val activityDetailCacheUpdatedAtFlow = MutableStateFlow<Long?>(null)
    private val detailIds = linkedSetOf<String>()
    private var staleActivityIds: Set<String> = emptySet()

    fun setActivities(activities: List<BoschActivity>) {
        activitiesFlow.value = activities
        updateOverview()
    }

    fun setDetail(activityId: String) {
        detailIds += activityId
        updateOverview()
    }

    fun setStaleActivityIds(activityIds: Set<String>) {
        staleActivityIds = activityIds
    }

    fun setCacheTimestamps(activityUpdatedAt: Long?, bikeUpdatedAt: Long?, detailUpdatedAt: Long?) {
        activityCacheUpdatedAtFlow.value = activityUpdatedAt
        bikeCacheUpdatedAtFlow.value = bikeUpdatedAt
        activityDetailCacheUpdatedAtFlow.value = detailUpdatedAt
    }

    override fun observeCachedActivities(): Flow<List<BoschActivity>> = activitiesFlow.asStateFlow()
    override fun observeCachedBikes(): Flow<List<BoschBike>> = MutableStateFlow(emptyList<BoschBike>()).asStateFlow()
    override fun observeCachedActivityDetailCacheOverview(): Flow<ActivityDetailCacheOverview> = detailOverviewFlow.asStateFlow()
    override fun observeActivityCacheUpdatedAtEpochMillis(): Flow<Long?> = activityCacheUpdatedAtFlow.asStateFlow()
    override fun observeBikeCacheUpdatedAtEpochMillis(): Flow<Long?> = bikeCacheUpdatedAtFlow.asStateFlow()
    override fun observeActivityDetailCacheUpdatedAtEpochMillis(): Flow<Long?> = activityDetailCacheUpdatedAtFlow.asStateFlow()
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
    override suspend fun getCachedActivityTotalCount(): Int? = activitiesFlow.value.size
    override suspend fun hasFreshActivities(maxAgeMillis: Long): Boolean = false
    override suspend fun hasFreshActivityDetail(activityId: String, maxAgeMillis: Long): Boolean = false
    override suspend fun hasFreshBikes(maxAgeMillis: Long): Boolean = false
    override suspend fun hasFreshBikeDetail(bikeId: String, maxAgeMillis: Long): Boolean = false
    override suspend fun getActivityIdsNeedingDetailSync(
        detailMode: CloudSyncDetailMode,
        staleThresholdEpochMillis: Long,
    ): List<String> = activitiesFlow.value.map { it.id }.filter { activityId ->
        val hasDetail = detailIds.contains(activityId)
        when (detailMode) {
            CloudSyncDetailMode.MISSING_ONLY -> !hasDetail
            CloudSyncDetailMode.MISSING_OR_STALE -> !hasDetail || staleActivityIds.contains(activityId)
        }
    }

    private fun updateOverview() {
        detailOverviewFlow.value = ActivityDetailCacheOverview(
            detailedActivityCount = detailIds.size,
            detailPointCount = 0,
            gpsPointCount = 0,
        )
    }
}
