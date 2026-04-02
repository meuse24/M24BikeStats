package info.meuse24.m24bikestats.domain.repository

import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.model.BoschBike

interface BoschSmartSystemRepository {
    suspend fun getActivities(accessToken: String): Result<List<BoschActivity>>
    suspend fun getBikes(accessToken: String): Result<List<BoschBike>>
    suspend fun getBikeDetail(accessToken: String, bikeId: String): Result<BoschBike>
}
