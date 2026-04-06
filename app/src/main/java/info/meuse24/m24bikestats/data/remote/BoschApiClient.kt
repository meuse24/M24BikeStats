package info.meuse24.m24bikestats.data.remote

import info.meuse24.m24bikestats.shared.TokenInfoFormat
import info.meuse24.m24bikestats.shared.decodeJwtParts
import info.meuse24.m24bikestats.api.BoschRequest
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
            val parts = decodeJwtParts(token)
                ?: return "Kein gültiges JWT"

            TokenInfoFormat.format(header = parts.header, payload = parts.payload)
        } catch (e: Exception) {
            "JWT-Decodierung fehlgeschlagen: ${e.message}"
        }
    }
}
