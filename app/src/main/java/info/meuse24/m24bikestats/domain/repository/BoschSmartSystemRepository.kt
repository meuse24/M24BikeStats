package info.meuse24.m24bikestats.domain.repository

import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.model.BoschActivityPage
import info.meuse24.m24bikestats.domain.model.BoschActivityDetail
import info.meuse24.m24bikestats.domain.model.BoschBike
import kotlinx.coroutines.flow.Flow

interface BoschSmartSystemRepository {
    fun observeCachedActivities(): Flow<List<BoschActivity>>
    fun observeCachedBikes(): Flow<List<BoschBike>>
    fun observeCachedActivityDetail(activityId: String): Flow<BoschActivityDetail?>
    fun observeCachedBike(bikeId: String): Flow<BoschBike?>
    suspend fun getCachedActivities(): List<BoschActivity>
    suspend fun getCachedActivity(activityId: String): BoschActivity?
    suspend fun getCachedActivityDetail(activityId: String): BoschActivityDetail?
    suspend fun getCachedBike(bikeId: String): BoschBike?
    suspend fun getActivities(accessToken: String, limit: Int, offset: Int): Result<BoschActivityPage>
    suspend fun getActivityDetail(accessToken: String, activityId: String): Result<BoschActivityDetail>
    suspend fun getBikes(accessToken: String): Result<List<BoschBike>>
    suspend fun getBikeDetail(accessToken: String, bikeId: String): Result<BoschBike>
}
