package info.meuse24.m24bikestats.data.remote

import info.meuse24.m24bikestats.domain.model.BoschRequest

interface BoschApiDataSource {
    suspend fun get(request: BoschRequest, accessToken: String): String
}
