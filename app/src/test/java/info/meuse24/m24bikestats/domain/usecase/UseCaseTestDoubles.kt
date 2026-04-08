package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.ActivityDetailCacheOverview
import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.model.BoschActivityDetail
import info.meuse24.m24bikestats.domain.model.BoschActivityPage
import info.meuse24.m24bikestats.domain.model.BoschBike
import info.meuse24.m24bikestats.domain.model.CloudSyncDetailMode
import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemCacheStatusRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class FakeAuthRepository(
    private val tokenResult: Result<String> = Result.success("token"),
) : AuthRepository {
    override fun getAccessToken(): String? = tokenResult.getOrNull()
    override suspend fun getValidAccessToken(): Result<String> = tokenResult
    override fun isAuthenticated(): Boolean = tokenResult.isSuccess
    override fun clearTokens() = Unit
}

internal open class FakeBoschSmartSystemRepository :
    BoschSmartSystemRepository,
    BoschSmartSystemCacheStatusRepository {
    var activitiesFresh = false
    var activityDetailFresh = false
    var bikesFresh = false
    var bikeDetailFresh = false

    var activitiesResult: Result<BoschActivityPage> = Result.failure(IllegalStateException("not stubbed"))
    var activityResultsByOffset: MutableMap<Int, Result<BoschActivityPage>> = mutableMapOf()
    var activityDetailResult: Result<BoschActivityDetail> = Result.failure(IllegalStateException("not stubbed"))
    var activityDetailResultsById: MutableMap<String, Result<BoschActivityDetail>> = mutableMapOf()
    var bikesResult: Result<List<BoschBike>> = Result.failure(IllegalStateException("not stubbed"))
    var bikeDetailResult: Result<BoschBike> = Result.failure(IllegalStateException("not stubbed"))

    var cachedActivities: List<BoschActivity> = emptyList()
    var cachedActivityTotalCount: Int? = null
    var cachedActivityDetails: MutableMap<String, BoschActivityDetail> = mutableMapOf()
    var staleActivityIds: MutableSet<String> = mutableSetOf()
    val activityCacheUpdatedAtFlow = MutableStateFlow<Long?>(null)
    val bikeCacheUpdatedAtFlow = MutableStateFlow<Long?>(null)
    val activityDetailCacheUpdatedAtFlow = MutableStateFlow<Long?>(null)

    var getActivitiesCalls = mutableListOf<Pair<Int, Int>>()
    var getActivityDetailCalls = mutableListOf<String>()
    var getBikesCalls = 0
    var getBikeDetailCalls = mutableListOf<String>()

    override fun observeCachedActivities(): Flow<List<BoschActivity>> = emptyFlow()
    override fun observeCachedBikes(): Flow<List<BoschBike>> = emptyFlow()
    override fun observeCachedActivityDetailCacheOverview(): Flow<ActivityDetailCacheOverview> =
        flowOf(
            ActivityDetailCacheOverview(
                detailedActivityCount = cachedActivityDetails.size,
                detailPointCount = cachedActivityDetails.values.sumOf { it.points.size },
                gpsPointCount = cachedActivityDetails.values.sumOf { detail ->
                    detail.points.count { point -> point.latitude != null && point.longitude != null }
                },
            )
        )
    override fun observeActivityCacheUpdatedAtEpochMillis(): Flow<Long?> = activityCacheUpdatedAtFlow.asStateFlow()
    override fun observeBikeCacheUpdatedAtEpochMillis(): Flow<Long?> = bikeCacheUpdatedAtFlow.asStateFlow()
    override fun observeActivityDetailCacheUpdatedAtEpochMillis(): Flow<Long?> = activityDetailCacheUpdatedAtFlow.asStateFlow()
    override fun observeCachedActivityDetail(activityId: String): Flow<BoschActivityDetail?> = emptyFlow()
    override fun observeCachedBike(bikeId: String): Flow<BoschBike?> = emptyFlow()
    override suspend fun getCachedActivities(): List<BoschActivity> = cachedActivities
    override suspend fun getCachedActivityTotalCount(): Int? = cachedActivityTotalCount
    override suspend fun getCachedActivity(activityId: String): BoschActivity? = cachedActivities.firstOrNull { it.id == activityId }
    override suspend fun getCachedActivityDetail(activityId: String): BoschActivityDetail? = cachedActivityDetails[activityId]
    override suspend fun getCachedBike(bikeId: String): BoschBike? = null
    override suspend fun hasFreshActivities(maxAgeMillis: Long): Boolean = activitiesFresh
    override suspend fun hasFreshActivityDetail(activityId: String, maxAgeMillis: Long): Boolean = activityDetailFresh
    override suspend fun hasFreshBikes(maxAgeMillis: Long): Boolean = bikesFresh
    override suspend fun hasFreshBikeDetail(bikeId: String, maxAgeMillis: Long): Boolean = bikeDetailFresh
    override suspend fun getActivityIdsNeedingDetailSync(
        detailMode: CloudSyncDetailMode,
        staleThresholdEpochMillis: Long,
    ): List<String> = cachedActivities.map { it.id }.filter { activityId ->
        val hasDetail = cachedActivityDetails.containsKey(activityId)
        when (detailMode) {
            CloudSyncDetailMode.MISSING_ONLY -> !hasDetail
            CloudSyncDetailMode.MISSING_OR_STALE ->
                !hasDetail || staleActivityIds.contains(activityId) || (staleActivityIds.isEmpty() && !activityDetailFresh)
        }
    }

    override suspend fun getActivities(accessToken: String, limit: Int, offset: Int): Result<BoschActivityPage> {
        getActivitiesCalls += limit to offset
        val result = activityResultsByOffset[offset] ?: activitiesResult
        result.onSuccess { page ->
            cachedActivityTotalCount = page.total
            val byId = cachedActivities.associateBy { it.id }.toMutableMap()
            page.items.forEach { activity -> byId[activity.id] = activity }
            cachedActivities = byId.values.toList()
        }
        return result
    }

    override suspend fun getActivityDetail(accessToken: String, activityId: String): Result<BoschActivityDetail> {
        getActivityDetailCalls += activityId
        val result = activityDetailResultsById[activityId] ?: activityDetailResult
        result.onSuccess { detail ->
            cachedActivityDetails[activityId] = detail
            staleActivityIds.remove(activityId)
            activityDetailCacheUpdatedAtFlow.value = activityDetailCacheUpdatedAtFlow.value ?: 1L
        }
        return result
    }

    override suspend fun getBikes(accessToken: String): Result<List<BoschBike>> {
        getBikesCalls += 1
        bikesResult.onSuccess {
            bikesFresh = true
            bikeCacheUpdatedAtFlow.value = bikeCacheUpdatedAtFlow.value ?: 1L
        }
        return bikesResult
    }

    override suspend fun getBikeDetail(accessToken: String, bikeId: String): Result<BoschBike> {
        getBikeDetailCalls += bikeId
        return bikeDetailResult
    }
}
