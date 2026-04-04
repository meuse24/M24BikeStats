package info.meuse24.m24bikestats.support.apitest

interface BoschRepository {
    suspend fun fetch(request: BoschRequest, accessToken: String): Result<String>
}
