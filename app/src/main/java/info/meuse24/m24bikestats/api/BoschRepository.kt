package info.meuse24.m24bikestats.api

interface BoschRepository {
    suspend fun fetch(request: BoschRequest, accessToken: String): Result<String>
}
