package info.meuse24.m24bikestats.data.remote

import android.util.Base64
import info.meuse24.m24bikestats.domain.model.BoschRequest
import info.meuse24.m24bikestats.shared.TokenInfoFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class BoschApiClient : BoschApiDataSource {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    override suspend fun get(request: BoschRequest, accessToken: String): String =
        withContext(Dispatchers.IO) {
            // TOKEN_INFO: kein HTTP-Call, Token lokal dekodieren
            if (request.isLocalOnly) {
                return@withContext decodeJwt(accessToken)
            }

            val httpRequest = Request.Builder()
                .url(request.url)
                .header("Authorization", "Bearer $accessToken")
                .header("Accept", "application/json")
                .build()

            client.newCall(httpRequest).execute().use { response ->
                val body = response.body?.string() ?: ""
                "HTTP ${response.code} ${response.message}\n\n$body"
            }
        }

    /**
     * Dekodiert den JWT-Payload (mittleres Segment) ohne Signaturprüfung.
     * Zeigt Scopes, Audience, Sub und Ablaufzeit des Access Tokens.
     */
    private fun decodeJwt(token: String): String {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return "Kein gültiges JWT (Segmente: ${parts.size})"

            val header  = String(Base64.decode(parts[0], Base64.URL_SAFE or Base64.NO_PADDING))
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING))

            TokenInfoFormat.format(header = header, payload = payload)
        } catch (e: Exception) {
            "JWT-Decodierung fehlgeschlagen: ${e.message}"
        }
    }
}
