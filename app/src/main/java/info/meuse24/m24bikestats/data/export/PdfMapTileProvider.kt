package info.meuse24.m24bikestats.data.export

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Looper
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
    fun getTileBitmap(
        zoom: Int,
        tileX: Int,
        tileY: Int,
    ): Bitmap? {
        if (Looper.myLooper() == Looper.getMainLooper()) return null
        val url = urlTemplate.format(zoom, tileX, tileY)
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "M24BikeStats PDF export")
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body ?: return@use null
                BitmapFactory.decodeStream(body.byteStream())
            }
        }.getOrNull()
    }
}
