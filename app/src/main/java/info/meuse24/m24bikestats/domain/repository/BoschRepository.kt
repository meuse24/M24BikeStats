package info.meuse24.m24bikestats.domain.repository

import info.meuse24.m24bikestats.domain.model.BoschRequest

interface BoschRepository {
    suspend fun fetch(request: BoschRequest, accessToken: String): Result<String>
}
