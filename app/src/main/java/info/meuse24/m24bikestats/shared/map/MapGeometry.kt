package info.meuse24.m24bikestats.shared.map

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

private const val MAX_MERCATOR_LATITUDE = 85.05112878

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
)

data class GeoBounds(
    val minLatitude: Double,
    val maxLatitude: Double,
    val minLongitude: Double,
    val maxLongitude: Double,
    val centerLatitude: Double,
    val centerLongitude: Double,
)

data class MapViewportPadding(
    val sidePaddingPx: Double,
    val topPaddingPx: Double,
    val bottomPaddingPx: Double,
)

data class MapTileViewport(
    val zoom: Int,
    val topLeftWorldX: Double,
    val topLeftWorldY: Double,
)

enum class BoundsCenterStrategy {
    MIDPOINT,
    AVERAGE_POINTS,
}

fun computeGeoBounds(
    points: List<GeoPoint>,
    centerStrategy: BoundsCenterStrategy = BoundsCenterStrategy.MIDPOINT,
): GeoBounds {
    require(points.isNotEmpty()) { "computeGeoBounds requires at least one point." }
    val minLatitude = points.minOf(GeoPoint::latitude)
    val maxLatitude = points.maxOf(GeoPoint::latitude)
    val minLongitude = points.minOf(GeoPoint::longitude)
    val maxLongitude = points.maxOf(GeoPoint::longitude)
    val centerLatitude = when (centerStrategy) {
        BoundsCenterStrategy.MIDPOINT -> (minLatitude + maxLatitude) / 2.0
        BoundsCenterStrategy.AVERAGE_POINTS -> points.map(GeoPoint::latitude).average()
    }
    val centerLongitude = when (centerStrategy) {
        BoundsCenterStrategy.MIDPOINT -> (minLongitude + maxLongitude) / 2.0
        BoundsCenterStrategy.AVERAGE_POINTS -> points.map(GeoPoint::longitude).average()
    }
    return GeoBounds(
        minLatitude = minLatitude,
        maxLatitude = maxLatitude,
        minLongitude = minLongitude,
        maxLongitude = maxLongitude,
        centerLatitude = centerLatitude,
        centerLongitude = centerLongitude,
    )
}

fun inflateGeoBounds(
    bounds: GeoBounds,
    spanScale: Double,
    minLatitudeSpan: Double,
    minLongitudeSpan: Double = minLatitudeSpan,
): GeoBounds {
    val paddedLatitudeSpan = ((bounds.maxLatitude - bounds.minLatitude).coerceAtLeast(minLatitudeSpan) * spanScale)
    val paddedLongitudeSpan = ((bounds.maxLongitude - bounds.minLongitude).coerceAtLeast(minLongitudeSpan) * spanScale)
    val minLatitude = (bounds.centerLatitude - paddedLatitudeSpan / 2.0).coerceIn(-MAX_MERCATOR_LATITUDE, MAX_MERCATOR_LATITUDE)
    val maxLatitude = (bounds.centerLatitude + paddedLatitudeSpan / 2.0).coerceIn(-MAX_MERCATOR_LATITUDE, MAX_MERCATOR_LATITUDE)
    val minLongitude = (bounds.centerLongitude - paddedLongitudeSpan / 2.0).coerceIn(-180.0, 180.0)
    val maxLongitude = (bounds.centerLongitude + paddedLongitudeSpan / 2.0).coerceIn(-180.0, 180.0)
    return GeoBounds(
        minLatitude = minLatitude,
        maxLatitude = maxLatitude,
        minLongitude = minLongitude,
        maxLongitude = maxLongitude,
        centerLatitude = (minLatitude + maxLatitude) / 2.0,
        centerLongitude = (minLongitude + maxLongitude) / 2.0,
    )
}

fun estimateZoomToFit(
    bounds: GeoBounds,
    viewportWidthPx: Double,
    viewportHeightPx: Double,
    tileSize: Double,
    padding: MapViewportPadding,
    minCoordinateDelta: Double,
    zoomAdjustment: Double,
    minZoom: Double,
    maxZoom: Double,
): Double {
    val usableWidth = (viewportWidthPx - (padding.sidePaddingPx * 2.0)).coerceAtLeast(1.0)
    val usableHeight = (viewportHeightPx - padding.topPaddingPx - padding.bottomPaddingPx).coerceAtLeast(1.0)
    val longitudeDelta = (bounds.maxLongitude - bounds.minLongitude).coerceAtLeast(minCoordinateDelta)
    val latitudeFraction = abs(mercatorYNormalized(bounds.maxLatitude) - mercatorYNormalized(bounds.minLatitude))
        .coerceAtLeast(minCoordinateDelta)
    val longitudeZoom = ln(usableWidth * 360.0 / (longitudeDelta * tileSize)) / ln(2.0)
    val latitudeZoom = ln(usableHeight / (latitudeFraction * tileSize)) / ln(2.0)
    return (minOf(longitudeZoom, latitudeZoom) - zoomAdjustment).coerceIn(minZoom, maxZoom)
}

fun fitZoomToPoints(
    points: List<GeoPoint>,
    centerLatitude: Double,
    centerLongitude: Double,
    initialZoom: Double,
    minZoom: Double,
    maxZoom: Double,
    viewportWidthPx: Double,
    viewportHeightPx: Double,
    marginFraction: Double,
    tileSize: Double,
    iterations: Int = 20,
): Double {
    fun fits(zoom: Double): Boolean =
        pointsFitInViewport(
            points = points,
            centerLatitude = centerLatitude,
            centerLongitude = centerLongitude,
            zoom = zoom,
            viewportWidthPx = viewportWidthPx,
            viewportHeightPx = viewportHeightPx,
            marginFraction = marginFraction,
            tileSize = tileSize,
        )

    val clampedInitialZoom = initialZoom.coerceIn(minZoom, maxZoom)
    if (fits(clampedInitialZoom)) return clampedInitialZoom
    if (!fits(minZoom)) return minZoom

    var low = minZoom
    var high = clampedInitialZoom
    repeat(iterations) {
        val mid = (low + high) / 2.0
        if (fits(mid)) {
            low = mid
        } else {
            high = mid
        }
    }
    return low
}

fun pointsFitInViewport(
    points: List<GeoPoint>,
    centerLatitude: Double,
    centerLongitude: Double,
    zoom: Double,
    viewportWidthPx: Double,
    viewportHeightPx: Double,
    marginFraction: Double,
    tileSize: Double,
): Boolean {
    val worldSize = tileSize * 2.0.pow(zoom)
    val centerX = longitudeToWorldX(centerLongitude, tileSize, zoom)
    val centerY = latitudeToWorldY(centerLatitude, tileSize, zoom)
    val minX = viewportWidthPx * marginFraction
    val maxX = viewportWidthPx * (1.0 - marginFraction)
    val minY = viewportHeightPx * marginFraction
    val maxY = viewportHeightPx * (1.0 - marginFraction)
    return points.all { point ->
        val pointWorldX = longitudeToWorldX(point.longitude, tileSize, zoom)
        val pointWorldY = latitudeToWorldY(point.latitude, tileSize, zoom)
        val deltaX = normalizeWrappedWorldDelta(pointWorldX - centerX, worldSize)
        val pointX = (viewportWidthPx / 2.0) + deltaX
        val pointY = (viewportHeightPx / 2.0) + (pointWorldY - centerY)
        pointX in minX..maxX && pointY in minY..maxY
    }
}

fun longitudeToWorldX(
    longitude: Double,
    tileSize: Double,
    zoom: Double,
): Double {
    val worldSize = tileSize * 2.0.pow(zoom)
    return ((longitude.coerceIn(-180.0, 180.0) + 180.0) / 360.0) * worldSize
}

fun latitudeToWorldY(
    latitude: Double,
    tileSize: Double,
    zoom: Double,
): Double {
    val worldSize = tileSize * 2.0.pow(zoom)
    return mercatorYNormalized(latitude) * worldSize
}

fun mercatorYNormalized(latitude: Double): Double {
    val clamped = latitude.coerceIn(-MAX_MERCATOR_LATITUDE, MAX_MERCATOR_LATITUDE)
    val radians = Math.toRadians(clamped)
    return (1.0 - ln(tan(radians) + (1.0 / cos(radians))) / PI) / 2.0
}

fun inverseMercatorYNormalized(normalizedY: Double): Double {
    val clamped = normalizedY.coerceIn(0.0, 1.0)
    val n = PI * (1.0 - 2.0 * clamped)
    return Math.toDegrees(kotlin.math.atan(kotlin.math.sinh(n)))
}

fun normalizeWrappedWorldDelta(
    delta: Double,
    worldSize: Double,
): Double {
    var wrapped = delta
    val half = worldSize / 2.0
    if (wrapped > half) wrapped -= worldSize
    if (wrapped < -half) wrapped += worldSize
    return wrapped
}

fun createDiscreteMapViewport(
    bounds: GeoBounds,
    mapWidthPx: Double,
    mapHeightPx: Double,
    tileSize: Double,
    minCoordinateDelta: Double,
    zoomAdjustment: Double,
    minZoom: Double,
    maxZoom: Double,
): MapTileViewport {
    // PDF rendering does not need interactive viewport padding. The caller is expected
    // to pre-inflate bounds before this step, then fit the discrete tile viewport to it.
    val zoom = kotlin.math.floor(
        estimateZoomToFit(
            bounds = bounds,
            viewportWidthPx = mapWidthPx,
            viewportHeightPx = mapHeightPx,
            tileSize = tileSize,
            padding = MapViewportPadding(0.0, 0.0, 0.0),
            minCoordinateDelta = minCoordinateDelta,
            zoomAdjustment = zoomAdjustment,
            minZoom = minZoom,
            maxZoom = maxZoom,
        ),
    ).toInt()
    val centerWorldX = longitudeToWorldX(bounds.centerLongitude, tileSize, zoom.toDouble())
    val centerWorldY = latitudeToWorldY(bounds.centerLatitude, tileSize, zoom.toDouble())
    return MapTileViewport(
        zoom = zoom,
        topLeftWorldX = centerWorldX - (mapWidthPx / 2.0),
        topLeftWorldY = centerWorldY - (mapHeightPx / 2.0),
    )
}
