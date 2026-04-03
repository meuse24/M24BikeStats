package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.model.BoschActivityDetail
import info.meuse24.m24bikestats.domain.model.BoschActivityPage
import info.meuse24.m24bikestats.domain.model.BoschBike
import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

internal class FakeAuthRepository(
    private val tokenResult: Result<String> = Result.success("token"),
) : AuthRepository {
    override fun getAccessToken(): String? = tokenResult.getOrNull()
    override suspend fun getValidAccessToken(): Result<String> = tokenResult
    override fun isAuthenticated(): Boolean = tokenResult.isSuccess
    override fun clearTokens() = Unit
}

internal open class FakeBoschSmartSystemRepository : BoschSmartSystemRepository {
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

    var getActivitiesCalls = mutableListOf<Pair<Int, Int>>()
    var getActivityDetailCalls = mutableListOf<String>()
    var getBikesCalls = 0
    var getBikeDetailCalls = mutableListOf<String>()

    override fun observeCachedActivities(): Flow<List<BoschActivity>> = emptyFlow()
    override fun observeCachedBikes(): Flow<List<BoschBike>> = emptyFlow()
    override fun observeCachedActivityDetail(activityId: String): Flow<BoschActivityDetail?> = emptyFlow()
    override fun observeCachedBike(bikeId: String): Flow<BoschBike?> = emptyFlow()
    override suspend fun getCachedActivities(): List<BoschActivity> = cachedActivities
    override suspend fun getCachedActivityTotalCount(): Int? = cachedActivityTotalCount
    override suspend fun getCachedActivity(activityId: String): BoschActivity? = cachedActivities.firstOrNull { it.id == activityId }
    override suspend fun getCachedActivityDetail(activityId: String): BoschActivityDetail? = cachedActivityDetails[activityId]
    override suspend fun getCachedBike(bikeId: String): BoschBike? = null
    override suspend fun isActivitiesCacheFresh(maxAgeMillis: Long): Boolean = activitiesFresh
    override suspend fun isActivityDetailCacheFresh(activityId: String, maxAgeMillis: Long): Boolean = activityDetailFresh
    override suspend fun isBikesCacheFresh(maxAgeMillis: Long): Boolean = bikesFresh
    override suspend fun isBikeDetailCacheFresh(bikeId: String, maxAgeMillis: Long): Boolean = bikeDetailFresh

    override suspend fun getActivities(accessToken: String, limit: Int, offset: Int): Result<BoschActivityPage> {
        getActivitiesCalls += limit to offset
        return activityResultsByOffset[offset] ?: activitiesResult
    }

    override suspend fun getActivityDetail(accessToken: String, activityId: String): Result<BoschActivityDetail> {
        getActivityDetailCalls += activityId
        return activityDetailResultsById[activityId] ?: activityDetailResult
    }

    override suspend fun getBikes(accessToken: String): Result<List<BoschBike>> {
        getBikesCalls += 1
        return bikesResult
    }

    override suspend fun getBikeDetail(accessToken: String, bikeId: String): Result<BoschBike> {
        getBikeDetailCalls += bikeId
        return bikeDetailResult
    }
}
