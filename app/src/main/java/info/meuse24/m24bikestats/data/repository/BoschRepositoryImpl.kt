package info.meuse24.m24bikestats.data.repository

import info.meuse24.m24bikestats.data.remote.BoschApiDataSource
import info.meuse24.m24bikestats.domain.model.BoschApiRequest
import info.meuse24.m24bikestats.domain.repository.BoschApiRepository

class BoschRepositoryImpl(private val apiClient: BoschApiDataSource) : BoschApiRepository {
    override suspend fun fetch(request: BoschApiRequest, accessToken: String): Result<String> =
        runCatching { apiClient.get(request, accessToken) }
}
