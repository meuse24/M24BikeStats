package info.meuse24.m24bikestats.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.auth.LoginRepository
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.CodeVerifierUtil
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenRequest

/**
 * Verwaltet den OAuth2 Authorization Code Flow gegen das Bosch eBike Portal (PKCE).
 * Implementiert [AuthRepository] für die Domain-Schicht.
 *
 * Nutzung:
 * 1. [buildAuthIntent] → Intent mit ActivityResultLauncher starten
 * 2. Ergebnis in [handleAuthResponse] übergeben
 * 3. Token über [getAccessToken] abrufbar
 */
class AuthManager(private val context: Context) : LoginRepository {

    private val authService = AuthorizationService(context)

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "bosch_tokens",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // --- OAuth2 Flow (plattformspezifisch, nicht im Interface) ---

    override fun buildAuthIntent(): Intent {
        val codeVerifier = CodeVerifierUtil.generateRandomCodeVerifier()
        val request = AuthorizationRequest.Builder(
            OAuthConfig.serviceConfiguration,
            OAuthConfig.CLIENT_ID,
            ResponseTypeValues.CODE,
            Uri.parse(OAuthConfig.REDIRECT_URI)
        )
            .setScopes(OAuthConfig.SCOPES)
            .setCodeVerifier(codeVerifier)
            .build()
        return authService.getAuthorizationRequestIntent(request)
    }

    override fun handleAuthResponse(
        intent: Intent,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val response = AuthorizationResponse.fromIntent(intent)
        val exception = AuthorizationException.fromIntent(intent)
        when {
            exception != null -> onError("Auth-Fehler: ${exception.errorDescription}")
            response != null -> exchangeCodeForToken(response, onSuccess, onError)
            else -> onError("Unbekannte Antwort vom Auth-Server")
        }
    }

    private fun exchangeCodeForToken(
        response: AuthorizationResponse,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val tokenRequest: TokenRequest = response.createTokenExchangeRequest()
        authService.performTokenRequest(tokenRequest) { tokenResponse, ex ->
            when {
                ex != null -> onError("Token-Fehler: ${ex.errorDescription}")
                tokenResponse != null -> {
                    saveTokens(
                        accessToken = tokenResponse.accessToken ?: "",
                        refreshToken = tokenResponse.refreshToken ?: "",
                        expiresAt = tokenResponse.accessTokenExpirationTime ?: 0L
                    )
                    onSuccess()
                }
                else -> onError("Leere Token-Antwort")
            }
        }
    }

    // --- AuthRepository ---

    override fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    override fun isAuthenticated(): Boolean {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        if (token.isNullOrBlank()) return false
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        return expiresAt > System.currentTimeMillis() + EXPIRY_BUFFER_MS
    }

    override fun clearTokens() {
        prefs.edit().clear().apply()
    }

    // ---

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun dispose() {
        authService.dispose()
    }

    private fun saveTokens(accessToken: String, refreshToken: String, expiresAt: Long) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .apply()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val EXPIRY_BUFFER_MS = 60_000L
    }
}
