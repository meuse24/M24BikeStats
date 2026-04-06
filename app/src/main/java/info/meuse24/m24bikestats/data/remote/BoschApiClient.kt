package info.meuse24.m24bikestats.data.remote

import info.meuse24.m24bikestats.domain.model.BoschApiRequest
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

    override suspend fun get(request: BoschApiRequest, accessToken: String): String =
        withContext(Dispatchers.IO) {
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
}
