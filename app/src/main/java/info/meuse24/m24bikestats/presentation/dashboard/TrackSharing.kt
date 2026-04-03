package info.meuse24.m24bikestats.presentation.dashboard

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import info.meuse24.m24bikestats.domain.model.CsvDialect
import info.meuse24.m24bikestats.domain.model.CsvExportFormat
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

data class TrackExportMetadata(
    val title: String,
    val trackPointCount: Int,
    val profilePointCount: Int,
    val distanceLabel: String?,
    val startCoordinateLabel: String?,
    val endCoordinateLabel: String?,
)

fun createTrackGpxUri(
    context: Context,
    activity: ActivityDetailUiModel,
): Uri = createSharedTrackFileUri(context, "${sanitizeTrackFileName(activity.title)}.gpx", buildTrackGpx(activity))

fun createTrackCsvUri(
    context: Context,
    activity: ActivityDetailUiModel,
    format: CsvExportFormat,
): Uri = createSharedTrackFileUri(
    context,
    "${sanitizeTrackFileName(activity.title)}.csv",
    buildTrackCsv(activity, format),
)

fun buildTrackGpx(activity: ActivityDetailUiModel): String {
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

fun buildTrackCsv(
    activity: ActivityDetailUiModel,
    format: CsvExportFormat = CsvExportFormat.STANDARD_INTERNATIONAL,
    locale: Locale = Locale.getDefault(),
): String {
    val dialect = format.resolve(locale)
    val header = dialect.row(
        listOf(
            "point_index",
            "latitude",
            "longitude",
            "distance_meters",
            "altitude_meters",
            "speed_kmh",
            "cadence_rpm",
            "rider_power_watts",
        )
    )

    val rows = activity.trackPoints.mapIndexed { index, point ->
        val profilePoint = point.distanceMeters?.let { distance ->
            activity.profilePoints.minByOrNull { candidate ->
                kotlin.math.abs(candidate.distanceMeters - distance)
            }
        }

        dialect.row(
            listOf(
                index.toString(),
                point.latitude.toCsvValue(dialect),
                point.longitude.toCsvValue(dialect),
                point.distanceMeters.toCsvValue(dialect),
                point.altitudeMeters.toCsvValue(dialect),
                profilePoint?.speedKmh.toCsvValue(dialect),
                profilePoint?.cadenceRpm.toCsvValue(dialect),
                profilePoint?.riderPowerWatts.toCsvValue(dialect),
            )
        )
    }

    return buildString {
        appendLine(header)
        rows.forEach(::appendLine)
    }
}

fun buildTrackGeoJson(activity: ActivityDetailUiModel): String {
    val coordinates = activity.trackPoints.joinToString(",\n") { point ->
        val altitude = point.altitudeMeters?.let { ",$it" }.orEmpty()
        "          [${point.longitude}, ${point.latitude}$altitude]"
    }
    return """
        |{
        |  "type": "FeatureCollection",
        |  "features": [
        |    {
        |      "type": "Feature",
        |      "properties": {
        |        "name": "${escapeJson(activity.title)}"
        |      },
        |      "geometry": {
        |        "type": "LineString",
        |        "coordinates": [
        |$coordinates
        |        ]
        |      }
        |    }
        |  ]
        |}
    """.trimMargin()
}

fun buildTrackExportMetadata(activity: ActivityDetailUiModel): TrackExportMetadata {
    val startPoint = activity.trackPoints.firstOrNull()
    val endPoint = activity.trackPoints.lastOrNull()
    val distanceLabel = activity.summary.firstOrNull { it.first == "Distanz" }?.second
        ?: activity.trackPoints.lastOrNull()?.distanceMeters?.let { formatKilometers(it) }

    return TrackExportMetadata(
        title = activity.title,
        trackPointCount = activity.trackPoints.size,
        profilePointCount = activity.profilePoints.size,
        distanceLabel = distanceLabel,
        startCoordinateLabel = startPoint?.let { formatCoordinatePair(it.latitude, it.longitude) },
        endCoordinateLabel = endPoint?.let { formatCoordinatePair(it.latitude, it.longitude) },
    )
}

private fun escapeXml(value: String): String =
    value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

private fun escapeJson(value: String): String =
    value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")

private fun formatCoordinatePair(latitude: Double, longitude: Double): String =
    String.format(Locale.US, "%.5f, %.5f", latitude, longitude)

private fun formatKilometers(distanceMeters: Double): String =
    String.format(Locale.US, "%.1f km", distanceMeters / 1000.0)

private fun sanitizeTrackFileName(title: String): String =
    title
        .lowercase(Locale.US)
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifBlank { "activity-track" }

private fun createSharedTrackFileUri(
    context: Context,
    fileName: String,
    content: String,
): Uri {
    val exportDir = File(context.cacheDir, "shared_tracks").apply { mkdirs() }
    val file = File(exportDir, fileName)
    file.writeText(content)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}

private fun Double?.toCsvValue(dialect: CsvDialect): String = when {
    this == null -> ""
    this.isNaN() -> ""
    else -> dialect.formatDecimal(this, 6)
}
