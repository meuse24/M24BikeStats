package info.meuse24.m24bikestats.data.remote

import info.meuse24.m24bikestats.support.apitest.BoschRequest

interface BoschApiDataSource {
    suspend fun get(request: BoschRequest, accessToken: String): String
}
