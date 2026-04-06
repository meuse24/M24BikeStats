package info.meuse24.m24bikestats.data.export

import android.graphics.Color

data class PdfColorScheme(
    val primary: Int = Color.parseColor("#1C6E52"),
    val secondary: Int = Color.parseColor("#C66A00"),
    val accent: Int = Color.parseColor("#2F4B7C"),
    val textPrimary: Int = Color.parseColor("#182128"),
    val textSecondary: Int = Color.parseColor("#58636C"),
    val border: Int = Color.parseColor("#CBD5DC"),
    val surface: Int = Color.WHITE,
    val surfaceMuted: Int = Color.parseColor("#F3F6F8"),
    val highlight: Int = Color.parseColor("#E7F2EC"),
    val divider: Int = Color.parseColor("#D7E0E6"),
)
