package info.meuse24.m24bikestats.data.remote

import info.meuse24.m24bikestats.domain.model.BoschEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class BoschApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun get(endpoint: BoschEndpoint, accessToken: String): String =
        withContext(Dispatchers.IO) {
            val url = "${endpoint.baseUrl}${endpoint.path}"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $accessToken")
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                // Immer HTTP-Status mitsenden, damit 404 sofort sichtbar ist
                "HTTP ${response.code} ${response.message}\n\n$body"
            }
        }
}
