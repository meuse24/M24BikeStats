package info.meuse24.m24bikestats.data.export

import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint

class PdfTypography(
    colors: PdfColorScheme,
) {
    val coverTitle = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colors.primary
        textSize = 28f
        typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
    }
    val coverSubtitle = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colors.textSecondary
        textSize = 15f
    }
    val sectionTitle = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colors.primary
        textSize = 20f
        typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
    }
    val heading = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colors.textPrimary
        textSize = 14f
        typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
    }
    val label = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colors.textSecondary
        textSize = 9f
        typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
    }
    val value = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colors.textPrimary
        textSize = 11f
    }
    val body = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colors.textPrimary
        textSize = 10f
    }
    val bodyMuted = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colors.textSecondary
        textSize = 10f
    }
    val tileValue = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colors.textPrimary
        textSize = 16f
        typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
    }
    val tileLabel = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colors.textSecondary
        textSize = 9f
        typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
    }
    val chartLabel = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colors.textSecondary
        textSize = 8f
    }
    val footer = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colors.textSecondary
        textSize = 8f
    }
}
