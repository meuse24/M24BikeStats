package info.meuse24.m24bikestats.data.repository

import info.meuse24.m24bikestats.data.remote.BoschApiDataSource
import info.meuse24.m24bikestats.api.BoschRepository
import info.meuse24.m24bikestats.api.BoschRequest

class BoschRepositoryImpl(private val apiClient: BoschApiDataSource) : BoschRepository {
    override suspend fun fetch(request: BoschRequest, accessToken: String): Result<String> =
        runCatching { apiClient.get(request, accessToken) }
}
