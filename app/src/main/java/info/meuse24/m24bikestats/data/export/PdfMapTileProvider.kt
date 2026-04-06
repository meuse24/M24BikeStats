package info.meuse24.m24bikestats.data.export

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class PdfMapTileProvider(
    private val urlTemplate: String = "https://tile.openstreetmap.org/%d/%d/%d.png",
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(1200, TimeUnit.MILLISECONDS)
        .readTimeout(1200, TimeUnit.MILLISECONDS)
        .callTimeout(1800, TimeUnit.MILLISECONDS)
        .build(),
) {
    private val memoryCache = object : LinkedHashMap<String, Bitmap>(96, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean = size > 96
    }

    @Synchronized
    fun getTileBitmap(
        zoom: Int,
        tileX: Int,
        tileY: Int,
    ): Bitmap? {
        val key = "$zoom/$tileX/$tileY"
        memoryCache[key]?.let { return it }
        val url = urlTemplate.format(zoom, tileX, tileY)
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "M24BikeStats PDF export")
            .build()
        val bitmap = runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body ?: return@use null
                BitmapFactory.decodeStream(body.byteStream())
            }
        }.getOrNull() ?: return null
        memoryCache[key] = bitmap
        return bitmap
    }
}
