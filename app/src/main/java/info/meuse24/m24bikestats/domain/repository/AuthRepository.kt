package info.meuse24.m24bikestats.domain.repository

interface AuthRepository {
    fun getAccessToken(): String?
    fun isAuthenticated(): Boolean
    fun clearTokens()
}
