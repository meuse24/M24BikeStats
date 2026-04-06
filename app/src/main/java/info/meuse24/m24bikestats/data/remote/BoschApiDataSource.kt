package info.meuse24.m24bikestats.data.remote

import info.meuse24.m24bikestats.domain.model.BoschApiRequest

interface BoschApiDataSource {
    suspend fun get(request: BoschApiRequest, accessToken: String): String
}
