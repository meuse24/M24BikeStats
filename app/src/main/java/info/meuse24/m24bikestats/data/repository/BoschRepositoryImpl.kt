package info.meuse24.m24bikestats.data.repository

import info.meuse24.m24bikestats.data.remote.BoschApiClient
import info.meuse24.m24bikestats.domain.model.BoschEndpoint
import info.meuse24.m24bikestats.domain.repository.BoschRepository

class BoschRepositoryImpl(private val apiClient: BoschApiClient) : BoschRepository {
    override suspend fun fetch(endpoint: BoschEndpoint, accessToken: String): Result<String> =
        runCatching { apiClient.get(endpoint, accessToken) }
}
