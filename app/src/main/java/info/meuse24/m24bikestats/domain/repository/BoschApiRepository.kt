package info.meuse24.m24bikestats.domain.repository

import info.meuse24.m24bikestats.domain.model.BoschApiRequest

interface BoschApiRepository {
    suspend fun fetch(request: BoschApiRequest, accessToken: String): Result<String>
}
