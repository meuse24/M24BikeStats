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
            val url = "${baseUrl(endpoint)}${endpoint.path}"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $accessToken")
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) "HTTP ${response.code} ${response.message}\n\n$body"
                else body.ifBlank { "(Leere Antwort)" }
            }
        }

    private fun baseUrl(endpoint: BoschEndpoint) = when (endpoint) {
        BoschEndpoint.USERINFO -> "https://p9.authz.bosch.com"
        else -> "https://api.bosch-ebike.com"
    }
}
