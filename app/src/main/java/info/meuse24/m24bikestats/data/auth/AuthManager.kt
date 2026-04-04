package info.meuse24.m24bikestats.data.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import info.meuse24.m24bikestats.auth.AuthFlowCoordinator
import info.meuse24.m24bikestats.domain.repository.AuthRepository
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.CodeVerifierUtil
import net.openid.appauth.EndSessionRequest
import net.openid.appauth.EndSessionResponse
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.openid.appauth.AuthorizationException
import kotlin.coroutines.resume

/**
 * Verwaltet den OAuth2 Authorization Code Flow gegen das Bosch eBike Portal (PKCE).
 * Implementiert [AuthRepository] für die Domain-Schicht.
 *
 * Nutzung:
 * 1. [buildAuthIntent] → Intent mit ActivityResultLauncher starten
 * 2. Ergebnis in [handleAuthResponse] übergeben
 * 3. Token über [getAccessToken] abrufbar
 */
class AuthManager(private val context: Context) : AuthRepository, AuthFlowCoordinator {

    private val authService = AuthorizationService(context)
    private val refreshMutex = Mutex()

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
                        expiresAt = tokenResponse.accessTokenExpirationTime ?: 0L,
                        idToken = tokenResponse.idToken
                    )
                    onSuccess()
                }
                else -> onError("Leere Token-Antwort")
            }
        }
    }

    // --- AuthRepository ---

    override fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    override suspend fun getValidAccessToken(): Result<String> = refreshMutex.withLock {
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        if (!accessToken.isNullOrBlank() && !isTokenExpiringSoon()) {
            return@withLock Result.success(accessToken)
        }

        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        if (refreshToken.isNullOrBlank()) {
            clearTokens()
            return@withLock Result.failure(
                IllegalStateException("Nicht angemeldet oder Sitzung abgelaufen – bitte erneut anmelden")
            )
        }

        refreshAccessToken(refreshToken)
    }

    override fun isAuthenticated(): Boolean {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        if (!token.isNullOrBlank() && !isTokenExpiringSoon()) return true
        return !refreshToken.isNullOrBlank()
    }

    override fun clearTokens() {
        prefs.edit().clear().apply()
    }

    override fun buildLogoutIntent(): Intent? {
        val endSessionEndpoint = OAuthConfig.serviceConfiguration.endSessionEndpoint ?: return null
        val requestBuilder = EndSessionRequest.Builder(OAuthConfig.serviceConfiguration)
            .setPostLogoutRedirectUri(Uri.parse(OAuthConfig.REDIRECT_URI))

        getIdToken()?.let(requestBuilder::setIdTokenHint)

        val request = requestBuilder.build()
        return authService.getEndSessionRequestIntent(request)
    }

    override fun handleLogoutResponse(
        intent: Intent?,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        val exception = intent?.let(AuthorizationException::fromIntent)
        val response = intent?.let(EndSessionResponse::fromIntent)

        clearTokens()

        when {
            exception != null -> onError("Logout-Fehler: ${exception.errorDescription ?: exception.error}")
            response != null || intent != null -> onComplete()
            else -> onComplete()
        }
    }

    private suspend fun refreshAccessToken(refreshToken: String): Result<String> =
        suspendCancellableCoroutine { continuation ->
            val request = TokenRequest.Builder(
                OAuthConfig.serviceConfiguration,
                OAuthConfig.CLIENT_ID
            )
                .setGrantType("refresh_token")
                .setRefreshToken(refreshToken)
                .build()

            authService.performTokenRequest(request) { tokenResponse, ex ->
                when {
                    ex != null -> {
                        clearTokens()
                        continuation.resume(
                            Result.failure(
                                IllegalStateException(
                                    "Token-Refresh fehlgeschlagen: ${ex.errorDescription ?: ex.error}"
                                )
                            )
                        )
                    }
                    tokenResponse?.accessToken.isNullOrBlank() -> {
                        clearTokens()
                        continuation.resume(
                            Result.failure(
                                IllegalStateException("Token-Refresh lieferte kein Access Token")
                            )
                        )
                    }
                    else -> {
                        val updatedRefreshToken = tokenResponse?.refreshToken ?: refreshToken
                        val expiresAt = tokenResponse?.accessTokenExpirationTime
                            ?: (System.currentTimeMillis() + DEFAULT_TOKEN_LIFETIME_MS)
                        val updatedAccessToken = tokenResponse?.accessToken.orEmpty()

                        saveTokens(
                            accessToken = updatedAccessToken,
                            refreshToken = updatedRefreshToken,
                            expiresAt = expiresAt,
                            idToken = tokenResponse?.idToken ?: getIdToken()
                        )
                        continuation.resume(Result.success(updatedAccessToken))
                    }
                }
            }
        }

    private fun isTokenExpiringSoon(): Boolean {
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        return expiresAt <= System.currentTimeMillis() + EXPIRY_BUFFER_MS
    }

    private fun getIdToken(): String? = prefs.getString(KEY_ID_TOKEN, null)

    private fun saveTokens(accessToken: String, refreshToken: String, expiresAt: Long, idToken: String?) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .putString(KEY_ID_TOKEN, idToken)
            .apply()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_ID_TOKEN = "id_token"
        private const val EXPIRY_BUFFER_MS = 60_000L
        private const val DEFAULT_TOKEN_LIFETIME_MS = 3_600_000L
    }
}
