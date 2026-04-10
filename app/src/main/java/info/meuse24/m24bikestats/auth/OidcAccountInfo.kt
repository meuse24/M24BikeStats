package info.meuse24.m24bikestats.auth

import info.meuse24.m24bikestats.api.BoschEndpoint
import info.meuse24.m24bikestats.domain.usecase.FetchBoschDataUseCase
import org.json.JSONObject

data class OidcUserInfoUiModel(
    val email: String?,
    val username: String?,
    val subject: String?,
)

interface OidcUserInfoProvider {
    suspend fun loadCurrentUserInfo(): OidcUserInfoUiModel?
}

class LiveOidcUserInfoProvider(
    private val fetchBoschData: FetchBoschDataUseCase,
    private val oidcCacheRepository: OidcCacheRepository,
) : OidcUserInfoProvider {
    override suspend fun loadCurrentUserInfo(): OidcUserInfoUiModel? {
        val liveUserInfo = fetchBoschData(BoschEndpoint.USERINFO.toRequest())
            .getOrNull()
            ?.let(::parseOidcUserInfo)
        if (liveUserInfo != null) {
            oidcCacheRepository.saveCachedUserInfo(liveUserInfo.toCachedUserInfo())
            return liveUserInfo
        }
        return oidcCacheRepository.getCachedUserInfo()?.toUiModel()
    }
}

data class OidcDiscoveryInfoUiModel(
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

interface OidcDiscoveryInfoProvider {
    suspend fun loadCurrentDiscovery(): OidcDiscoveryInfoUiModel?
}

class LiveOidcDiscoveryInfoProvider(
    private val fetchBoschData: FetchBoschDataUseCase,
    private val oidcCacheRepository: OidcCacheRepository,
) : OidcDiscoveryInfoProvider {
    override suspend fun loadCurrentDiscovery(): OidcDiscoveryInfoUiModel? {
        val liveDiscoveryInfo = fetchBoschData(BoschEndpoint.OIDC_DISCOVERY.toRequest())
            .getOrNull()
            ?.let(::parseOidcDiscoveryInfo)
        if (liveDiscoveryInfo != null) {
            oidcCacheRepository.saveCachedDiscoveryInfo(liveDiscoveryInfo.toCachedDiscoveryInfo())
            return liveDiscoveryInfo
        }
        return oidcCacheRepository.getCachedDiscoveryInfo()?.toUiModel()
    }
}

internal fun parseOidcUserInfo(response: String): OidcUserInfoUiModel? {
    val jsonBody = parseJsonBody(response) ?: return null
    return OidcUserInfoUiModel(
        email = jsonBody.optString("email").ifBlank { null },
        username = jsonBody.optString("preferred_username").ifBlank { null },
        subject = jsonBody.optString("sub").ifBlank { null },
    )
}

internal fun parseOidcDiscoveryInfo(response: String): OidcDiscoveryInfoUiModel? {
    val jsonBody = parseJsonBody(response) ?: return null
    return OidcDiscoveryInfoUiModel(
        issuer = jsonBody.optString("issuer").ifBlank { null },
        authorizationEndpoint = jsonBody.optString("authorization_endpoint").ifBlank { null },
        tokenEndpoint = jsonBody.optString("token_endpoint").ifBlank { null },
        userInfoEndpoint = jsonBody.optString("userinfo_endpoint").ifBlank { null },
        jwksUri = jsonBody.optString("jwks_uri").ifBlank { null },
        revocationEndpoint = jsonBody.optString("revocation_endpoint").ifBlank { null },
        introspectionEndpoint = jsonBody.optString("introspection_endpoint").ifBlank { null },
        endSessionEndpoint = jsonBody.optString("end_session_endpoint").ifBlank { null },
        supportedGrantTypes = jsonBody.optJSONArray("grant_types_supported")
            ?.let { array ->
                buildList {
                    for (index in 0 until array.length()) {
                        array.optString(index)
                            .takeIf(String::isNotBlank)
                            ?.let(::add)
                    }
                }
            }
            .orEmpty(),
    )
}

private fun parseJsonBody(response: String): JSONObject? =
    response.indexOf('{')
        .takeIf { it >= 0 }
        ?.let { bodyStartIndex ->
            runCatching {
                JSONObject(response.substring(bodyStartIndex).trim())
            }.getOrNull()
        }

private fun OidcUserInfoUiModel.toCachedUserInfo(): CachedOidcUserInfo =
    CachedOidcUserInfo(
        email = email,
        username = username,
        subject = subject,
    )

private fun CachedOidcUserInfo.toUiModel(): OidcUserInfoUiModel =
    OidcUserInfoUiModel(
        email = email,
        username = username,
        subject = subject,
    )

private fun OidcDiscoveryInfoUiModel.toCachedDiscoveryInfo(): CachedOidcDiscoveryInfo =
    CachedOidcDiscoveryInfo(
        issuer = issuer,
        authorizationEndpoint = authorizationEndpoint,
        tokenEndpoint = tokenEndpoint,
        userInfoEndpoint = userInfoEndpoint,
        jwksUri = jwksUri,
        revocationEndpoint = revocationEndpoint,
        introspectionEndpoint = introspectionEndpoint,
        endSessionEndpoint = endSessionEndpoint,
        supportedGrantTypes = supportedGrantTypes,
    )

private fun CachedOidcDiscoveryInfo.toUiModel(): OidcDiscoveryInfoUiModel =
    OidcDiscoveryInfoUiModel(
        issuer = issuer,
        authorizationEndpoint = authorizationEndpoint,
        tokenEndpoint = tokenEndpoint,
        userInfoEndpoint = userInfoEndpoint,
        jwksUri = jwksUri,
        revocationEndpoint = revocationEndpoint,
        introspectionEndpoint = introspectionEndpoint,
        endSessionEndpoint = endSessionEndpoint,
        supportedGrantTypes = supportedGrantTypes,
    )
