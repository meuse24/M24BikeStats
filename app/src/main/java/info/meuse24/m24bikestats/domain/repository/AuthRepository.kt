package info.meuse24.m24bikestats.domain.repository

interface AuthRepository {
    fun getAccessToken(): String?
    suspend fun getValidAccessToken(): Result<String>
    fun isAuthenticated(): Boolean
    fun clearTokens()
}
