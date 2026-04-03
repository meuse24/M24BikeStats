package info.meuse24.m24bikestats.data.repository

import info.meuse24.m24bikestats.data.remote.BoschApiDataSource
import info.meuse24.m24bikestats.domain.model.BoschRequest
import info.meuse24.m24bikestats.domain.repository.BoschRepository

class BoschRepositoryImpl(private val apiClient: BoschApiDataSource) : BoschRepository {
    override suspend fun fetch(request: BoschRequest, accessToken: String): Result<String> =
        runCatching { apiClient.get(request, accessToken) }
}
