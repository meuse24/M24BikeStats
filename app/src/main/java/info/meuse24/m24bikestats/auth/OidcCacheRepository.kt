package info.meuse24.m24bikestats.auth

interface OidcCacheRepository {
    fun getCachedUserInfo(): CachedOidcUserInfo?
    fun saveCachedUserInfo(info: CachedOidcUserInfo)
    fun getCachedDiscoveryInfo(): CachedOidcDiscoveryInfo?
    fun saveCachedDiscoveryInfo(info: CachedOidcDiscoveryInfo)
    fun clearOidcCache()
}

data class CachedOidcUserInfo(
    val email: String?,
    val username: String?,
    val subject: String?,
)

data class CachedOidcDiscoveryInfo(
    val issuer: String?,
    val authorizationEndpoint: String?,
    val tokenEndpoint: String?,
    val userInfoEndpoint: String?,
    val jwksUri: String?,
    val revocationEndpoint: String?,
    val introspectionEndpoint: String?,
    val endSessionEndpoint: String?,
    val supportedGrantTypes: List<String>,
)
