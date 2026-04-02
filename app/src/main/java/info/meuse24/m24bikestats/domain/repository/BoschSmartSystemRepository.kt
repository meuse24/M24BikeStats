package info.meuse24.m24bikestats.domain.repository

import info.meuse24.m24bikestats.domain.model.BoschActivityPage
import info.meuse24.m24bikestats.domain.model.BoschActivityDetail
import info.meuse24.m24bikestats.domain.model.BoschBike

interface BoschSmartSystemRepository {
    suspend fun getActivities(accessToken: String, limit: Int, offset: Int): Result<BoschActivityPage>
    suspend fun getActivityDetail(accessToken: String, activityId: String): Result<BoschActivityDetail>
    suspend fun getBikes(accessToken: String): Result<List<BoschBike>>
    suspend fun getBikeDetail(accessToken: String, bikeId: String): Result<BoschBike>
}
