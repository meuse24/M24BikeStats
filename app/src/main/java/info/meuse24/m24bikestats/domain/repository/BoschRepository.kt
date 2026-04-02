package info.meuse24.m24bikestats.domain.repository

import info.meuse24.m24bikestats.domain.model.BoschEndpoint

interface BoschRepository {
    suspend fun fetch(endpoint: BoschEndpoint, accessToken: String): Result<String>
}
