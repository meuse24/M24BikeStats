package info.meuse24.m24bikestats.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import info.meuse24.m24bikestats.auth.CachedOidcDiscoveryInfo
import info.meuse24.m24bikestats.auth.CachedOidcUserInfo
import info.meuse24.m24bikestats.auth.OidcCacheRepository

class OidcCacheRepositoryImpl(
    context: Context,
) : OidcCacheRepository {

    private val preferences: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFERENCES_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override fun getCachedUserInfo(): CachedOidcUserInfo? {
        val email = preferences.getString(KEY_USER_EMAIL, null)
        val username = preferences.getString(KEY_USER_USERNAME, null)
        val subject = preferences.getString(KEY_USER_SUBJECT, null)
        if (email == null && username == null && subject == null) return null
        return CachedOidcUserInfo(
            email = email,
            username = username,
            subject = subject,
        )
    }

    override fun saveCachedUserInfo(info: CachedOidcUserInfo) {
        preferences.edit()
            .putString(KEY_USER_EMAIL, info.email)
            .putString(KEY_USER_USERNAME, info.username)
            .putString(KEY_USER_SUBJECT, info.subject)
            .apply()
    }

    override fun getCachedDiscoveryInfo(): CachedOidcDiscoveryInfo? {
        val issuer = preferences.getString(KEY_DISCOVERY_ISSUER, null)
        val authorizationEndpoint = preferences.getString(KEY_DISCOVERY_AUTHORIZATION_ENDPOINT, null)
        val tokenEndpoint = preferences.getString(KEY_DISCOVERY_TOKEN_ENDPOINT, null)
        val userInfoEndpoint = preferences.getString(KEY_DISCOVERY_USERINFO_ENDPOINT, null)
        val jwksUri = preferences.getString(KEY_DISCOVERY_JWKS_URI, null)
        val revocationEndpoint = preferences.getString(KEY_DISCOVERY_REVOCATION_ENDPOINT, null)
        val introspectionEndpoint = preferences.getString(KEY_DISCOVERY_INTROSPECTION_ENDPOINT, null)
        val endSessionEndpoint = preferences.getString(KEY_DISCOVERY_END_SESSION_ENDPOINT, null)
        val supportedGrantTypes = preferences.getString(KEY_DISCOVERY_SUPPORTED_GRANT_TYPES, null)
            ?.split(',')
            ?.map(String::trim)
            ?.filter(String::isNotEmpty)
            .orEmpty()
        if (
            issuer == null &&
            authorizationEndpoint == null &&
            tokenEndpoint == null &&
            userInfoEndpoint == null &&
            jwksUri == null &&
            revocationEndpoint == null &&
            introspectionEndpoint == null &&
            endSessionEndpoint == null &&
            supportedGrantTypes.isEmpty()
        ) {
            return null
        }
        return CachedOidcDiscoveryInfo(
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
    }

    override fun saveCachedDiscoveryInfo(info: CachedOidcDiscoveryInfo) {
        preferences.edit()
            .putString(KEY_DISCOVERY_ISSUER, info.issuer)
            .putString(KEY_DISCOVERY_AUTHORIZATION_ENDPOINT, info.authorizationEndpoint)
            .putString(KEY_DISCOVERY_TOKEN_ENDPOINT, info.tokenEndpoint)
            .putString(KEY_DISCOVERY_USERINFO_ENDPOINT, info.userInfoEndpoint)
            .putString(KEY_DISCOVERY_JWKS_URI, info.jwksUri)
            .putString(KEY_DISCOVERY_REVOCATION_ENDPOINT, info.revocationEndpoint)
            .putString(KEY_DISCOVERY_INTROSPECTION_ENDPOINT, info.introspectionEndpoint)
            .putString(KEY_DISCOVERY_END_SESSION_ENDPOINT, info.endSessionEndpoint)
            .putString(KEY_DISCOVERY_SUPPORTED_GRANT_TYPES, info.supportedGrantTypes.joinToString(","))
            .apply()
    }

    override fun clearOidcCache() {
        preferences.edit().clear().apply()
    }

    private companion object {
        private const val PREFERENCES_NAME = "bosch_oidc_cache"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_USERNAME = "user_username"
        private const val KEY_USER_SUBJECT = "user_subject"
        private const val KEY_DISCOVERY_ISSUER = "discovery_issuer"
        private const val KEY_DISCOVERY_AUTHORIZATION_ENDPOINT = "discovery_authorization_endpoint"
        private const val KEY_DISCOVERY_TOKEN_ENDPOINT = "discovery_token_endpoint"
        private const val KEY_DISCOVERY_USERINFO_ENDPOINT = "discovery_userinfo_endpoint"
        private const val KEY_DISCOVERY_JWKS_URI = "discovery_jwks_uri"
        private const val KEY_DISCOVERY_REVOCATION_ENDPOINT = "discovery_revocation_endpoint"
        private const val KEY_DISCOVERY_INTROSPECTION_ENDPOINT = "discovery_introspection_endpoint"
        private const val KEY_DISCOVERY_END_SESSION_ENDPOINT = "discovery_end_session_endpoint"
        private const val KEY_DISCOVERY_SUPPORTED_GRANT_TYPES = "discovery_supported_grant_types"
    }
}
