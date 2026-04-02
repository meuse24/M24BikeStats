package info.meuse24.m24bikestats.presentation.dashboard

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

fun createTrackGpxUri(
    context: Context,
    activity: ActivityDetailUiModel,
): Uri {
    val exportDir = File(context.cacheDir, "shared_tracks").apply { mkdirs() }
    val safeName = activity.title
        .lowercase(Locale.US)
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifBlank { "activity-track" }
    val file = File(exportDir, "$safeName.gpx")
    file.writeText(buildGpx(activity))
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}

private fun buildGpx(activity: ActivityDetailUiModel): String {
    val timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now().atOffset(ZoneOffset.UTC))
    val points = activity.trackPoints.joinToString("\n") { point ->
        buildString {
            append("""      <trkpt lat="${point.latitude}" lon="${point.longitude}">""")
            point.altitudeMeters?.let { append("\n        <ele>$it</ele>") }
            point.distanceMeters?.let { append("\n        <extensions><distance>$it</distance></extensions>") }
            append("\n      </trkpt>")
        }
    }

    return """
        |<?xml version="1.0" encoding="UTF-8"?>
        |<gpx version="1.1" creator="M24BikeStats" xmlns="http://www.topografix.com/GPX/1/1">
        |  <metadata>
        |    <name>${escapeXml(activity.title)}</name>
        |    <time>$timestamp</time>
        |  </metadata>
        |  <trk>
        |    <name>${escapeXml(activity.title)}</name>
        |    <trkseg>
        |$points
        |    </trkseg>
        |  </trk>
        |</gpx>
    """.trimMargin()
}

private fun escapeXml(value: String): String =
    value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
