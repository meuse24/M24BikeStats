package info.meuse24.m24bikestats.domain.repository

import info.meuse24.m24bikestats.domain.model.ActivityDetailCacheMetadata
import info.meuse24.m24bikestats.domain.model.ActivityDetailCacheOverview
import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.model.BoschActivityPage
import info.meuse24.m24bikestats.domain.model.BoschActivityDetail
import info.meuse24.m24bikestats.domain.model.BoschBike
import kotlinx.coroutines.flow.Flow

interface BoschSmartSystemRepository {
    fun observeCachedActivities(): Flow<List<BoschActivity>>
    fun observeCachedBikes(): Flow<List<BoschBike>>
    fun observeCachedActivityDetailCacheOverview(): Flow<ActivityDetailCacheOverview>
    fun observeCachedActivityDetail(activityId: String): Flow<BoschActivityDetail?>
    fun observeCachedBike(bikeId: String): Flow<BoschBike?>
    suspend fun getCachedActivities(): List<BoschActivity>
    suspend fun getCachedActivityTotalCount(): Int?
    suspend fun getCachedActivity(activityId: String): BoschActivity?
    suspend fun getCachedActivityDetail(activityId: String): BoschActivityDetail?
    suspend fun getCachedActivityDetailMetadata(): List<ActivityDetailCacheMetadata>
    suspend fun getCachedBike(bikeId: String): BoschBike?
    suspend fun isActivitiesCacheFresh(maxAgeMillis: Long): Boolean
    suspend fun isActivityDetailCacheFresh(activityId: String, maxAgeMillis: Long): Boolean
    suspend fun isBikesCacheFresh(maxAgeMillis: Long): Boolean
    suspend fun isBikeDetailCacheFresh(bikeId: String, maxAgeMillis: Long): Boolean
    suspend fun getActivities(accessToken: String, limit: Int, offset: Int): Result<BoschActivityPage>
    suspend fun getActivityDetail(accessToken: String, activityId: String): Result<BoschActivityDetail>
    suspend fun getBikes(accessToken: String): Result<List<BoschBike>>
    suspend fun getBikeDetail(accessToken: String, bikeId: String): Result<BoschBike>
}
