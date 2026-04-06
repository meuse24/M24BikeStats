package info.meuse24.m24bikestats.presentation.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import info.meuse24.m24bikestats.R
import info.meuse24.m24bikestats.domain.model.ActivityMapPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Position
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val MAP_WORLD_TILE_SIZE = 512.0
private const val MAP_CLICK_TOLERANCE_PX = 30.0

@Composable
fun MapSummaryScreen(
    uiState: MapSummaryUiState,
    savedCameraPosition: CameraPosition?,
    onCameraPositionChanged: (CameraPosition) -> Unit,
    onActivityClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        when {
            uiState.isLoading -> CircularProgressIndicator()
            uiState.points.isEmpty() -> Text(stringResource(R.string.map_no_gps_data))
            else -> ActivityHeatMap(
                points = uiState.points,
                savedCameraPosition = savedCameraPosition,
                onCameraPositionChanged = onCameraPositionChanged,
                onActivityClick = onActivityClick,
            )
        }
    }
}

@Composable
private fun ActivityHeatMap(
    points: List<ActivityMapPoint>,
    savedCameraPosition: CameraPosition?,
    onCameraPositionChanged: (CameraPosition) -> Unit,
    onActivityClick: (String) -> Unit,
) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val viewportWidthPx = with(density) { maxWidth.toPx().toDouble().coerceAtLeast(1.0) }
        val viewportHeightPx = with(density) { maxHeight.toPx().toDouble().coerceAtLeast(1.0) }
        val autoFitPosition = remember(points, viewportWidthPx, viewportHeightPx) {
            calculateMapCameraPosition(
                points = points,
                viewportWidthPx = viewportWidthPx,
                viewportHeightPx = viewportHeightPx,
            )
        }
        val useSavedCamera = remember(savedCameraPosition, autoFitPosition, points, viewportWidthPx, viewportHeightPx) {
            savedCameraPosition != null && shouldUseSavedCameraPosition(
                saved = savedCameraPosition,
                autoFit = autoFitPosition,
                points = points,
                viewportWidthPx = viewportWidthPx,
                viewportHeightPx = viewportHeightPx,
            )
        }
        val initialPosition = remember(savedCameraPosition, autoFitPosition, useSavedCamera) {
            if (useSavedCamera) savedCameraPosition!! else autoFitPosition
        }
        val cameraState = rememberCameraState(firstPosition = initialPosition)

        LaunchedEffect(savedCameraPosition, autoFitPosition, useSavedCamera) {
            if (!useSavedCamera) {
                cameraState.position = autoFitPosition
            }
        }

        LaunchedEffect(cameraState) {
            snapshotFlow { cameraState.position }
                .distinctUntilChanged()
                .collect { onCameraPositionChanged(it) }
        }

        val geoJson = remember(points) {
            GeoJsonData.JsonString(ActivityMapPointGeoJsonMapper.toGeoJsonString(points))
        }

        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            baseStyle = BaseStyle.Uri(STYLE_URL),
            cameraState = cameraState,
            onMapClick = { position, _ ->
                val zoom = cameraState.position.zoom
                val clicked = points.nearestWithinTolerance(position.latitude, position.longitude, zoom)
                if (clicked != null) {
                    onActivityClick(clicked.activityId)
                    ClickResult.Consume
                } else {
                    ClickResult.Pass
                }
            },
        ) {
            val source = rememberGeoJsonSource(data = geoJson)
            CircleLayer(
                id = "activity-centers",
                source = source,
                color = const(Color(0xFF1565C0)),
                radius = const(6.dp),
                strokeColor = const(Color.White),
                strokeWidth = const(1.5.dp),
            )
        }
    }
}

private data class MapBounds(
    val minLatitude: Double,
    val maxLatitude: Double,
    val minLongitude: Double,
    val maxLongitude: Double,
    val centerLatitude: Double,
    val centerLongitude: Double,
)

private fun calculateMapCameraPosition(
    points: List<ActivityMapPoint>,
    viewportWidthPx: Double,
    viewportHeightPx: Double,
): CameraPosition {
    val rawBounds = points.toMapBounds()
    val bounds = inflateMapBounds(rawBounds)
    val zoom = estimateMapZoom(
        bounds = bounds,
        viewportWidthPx = viewportWidthPx,
        viewportHeightPx = viewportHeightPx,
    )
    val fittedZoom = adjustZoomToFitPoints(
        points = points,
        centerLatitude = bounds.centerLatitude,
        centerLongitude = bounds.centerLongitude,
        initialZoom = zoom,
        viewportWidthPx = viewportWidthPx,
        viewportHeightPx = viewportHeightPx,
    )
    return CameraPosition(
        target = Position(
            longitude = bounds.centerLongitude,
            latitude = bounds.centerLatitude,
        ),
        zoom = fittedZoom,
    )
}

private fun List<ActivityMapPoint>.toMapBounds(): MapBounds {
    val minLat = minOf { it.latitude }
    val maxLat = maxOf { it.latitude }
    val minLng = minOf { it.longitude }
    val maxLng = maxOf { it.longitude }
    val avgLat = map { it.latitude }.average()
    val avgLng = map { it.longitude }.average()
    return MapBounds(
        minLatitude = minLat,
        maxLatitude = maxLat,
        minLongitude = minLng,
        maxLongitude = maxLng,
        centerLatitude = avgLat,
        centerLongitude = avgLng,
    )
}

private fun inflateMapBounds(bounds: MapBounds): MapBounds {
    val latitudeSpan = (bounds.maxLatitude - bounds.minLatitude).coerceAtLeast(0.0008)
    val longitudeSpan = (bounds.maxLongitude - bounds.minLongitude).coerceAtLeast(0.0008)
    val paddedLatitudeSpan = (latitudeSpan * 1.22).coerceAtLeast(0.02)
    val paddedLongitudeSpan = (longitudeSpan * 1.22).coerceAtLeast(0.02)
    val centerLatitude = bounds.centerLatitude
    val centerLongitude = bounds.centerLongitude

    val minLatitude = (centerLatitude - paddedLatitudeSpan / 2.0).coerceIn(-85.05112878, 85.05112878)
    val maxLatitude = (centerLatitude + paddedLatitudeSpan / 2.0).coerceIn(-85.05112878, 85.05112878)
    val minLongitude = (centerLongitude - paddedLongitudeSpan / 2.0).coerceIn(-180.0, 180.0)
    val maxLongitude = (centerLongitude + paddedLongitudeSpan / 2.0).coerceIn(-180.0, 180.0)
    return MapBounds(
        minLatitude = minLatitude,
        maxLatitude = maxLatitude,
        minLongitude = minLongitude,
        maxLongitude = maxLongitude,
        centerLatitude = (minLatitude + maxLatitude) / 2.0,
        centerLongitude = (minLongitude + maxLongitude) / 2.0,
    )
}

private fun estimateMapZoom(
    bounds: MapBounds,
    viewportWidthPx: Double,
    viewportHeightPx: Double,
): Double {
    val sidePaddingPx = 36.0
    val topPaddingPx = 36.0
    val bottomPaddingPx = 36.0
    val usableWidth = (viewportWidthPx - (sidePaddingPx * 2.0)).coerceAtLeast(1.0)
    val usableHeight = (viewportHeightPx - topPaddingPx - bottomPaddingPx).coerceAtLeast(1.0)
    val longitudeDelta = (bounds.maxLongitude - bounds.minLongitude).coerceAtLeast(0.0003)
    val latitudeFraction = abs(mercatorY(bounds.maxLatitude) - mercatorY(bounds.minLatitude)).coerceAtLeast(0.0003)
    val longitudeZoom = ln(usableWidth * 360.0 / (longitudeDelta * MAP_WORLD_TILE_SIZE)) / ln(2.0)
    val latitudeZoom = ln(usableHeight / (latitudeFraction * MAP_WORLD_TILE_SIZE)) / ln(2.0)
    return (minOf(longitudeZoom, latitudeZoom) - 1.05).coerceIn(3.2, 14.0)
}

private fun shouldUseSavedCameraPosition(
    saved: CameraPosition,
    autoFit: CameraPosition,
    points: List<ActivityMapPoint>,
    viewportWidthPx: Double,
    viewportHeightPx: Double,
): Boolean {
    val zoomMatches = saved.zoom in (autoFit.zoom - 3.0)..(autoFit.zoom + 1.0)
    if (!zoomMatches) return false
    return pointsFitInViewport(
        points = points,
        centerLatitude = saved.target.latitude,
        centerLongitude = saved.target.longitude,
        zoom = saved.zoom,
        viewportWidthPx = viewportWidthPx,
        viewportHeightPx = viewportHeightPx,
        marginFraction = 0.10,
    )
}

private fun adjustZoomToFitPoints(
    points: List<ActivityMapPoint>,
    centerLatitude: Double,
    centerLongitude: Double,
    initialZoom: Double,
    viewportWidthPx: Double,
    viewportHeightPx: Double,
): Double {
    var zoom = initialZoom
    repeat(28) {
        if (
            pointsFitInViewport(
                points = points,
                centerLatitude = centerLatitude,
                centerLongitude = centerLongitude,
                zoom = zoom,
                viewportWidthPx = viewportWidthPx,
                viewportHeightPx = viewportHeightPx,
                marginFraction = 0.10,
            )
        ) {
            return zoom
        }
        zoom -= 0.3
        if (zoom <= 2.5) return 2.5
    }
    return zoom.coerceAtLeast(2.5)
}

private fun pointsFitInViewport(
    points: List<ActivityMapPoint>,
    centerLatitude: Double,
    centerLongitude: Double,
    zoom: Double,
    viewportWidthPx: Double,
    viewportHeightPx: Double,
    marginFraction: Double,
): Boolean {
    val worldSize = MAP_WORLD_TILE_SIZE * 2.0.pow(zoom)
    val centerX = longitudeToWorldX(centerLongitude, zoom)
    val centerY = latitudeToWorldY(centerLatitude, zoom)
    val minX = viewportWidthPx * marginFraction
    val maxX = viewportWidthPx * (1.0 - marginFraction)
    val minY = viewportHeightPx * marginFraction
    val maxY = viewportHeightPx * (1.0 - marginFraction)
    return points.all { point ->
        val pointWorldX = longitudeToWorldX(point.longitude, zoom)
        val pointWorldY = latitudeToWorldY(point.latitude, zoom)
        val deltaX = normalizeWorldDelta(pointWorldX - centerX, worldSize)
        val pointX = (viewportWidthPx / 2.0) + deltaX
        val pointY = (viewportHeightPx / 2.0) + (pointWorldY - centerY)
        pointX in minX..maxX && pointY in minY..maxY
    }
}

private fun normalizeWorldDelta(
    delta: Double,
    worldSize: Double,
): Double {
    var wrapped = delta
    val half = worldSize / 2.0
    if (wrapped > half) wrapped -= worldSize
    if (wrapped < -half) wrapped += worldSize
    return wrapped
}

private fun longitudeToWorldX(
    longitude: Double,
    zoom: Double,
): Double {
    val worldSize = MAP_WORLD_TILE_SIZE * 2.0.pow(zoom)
    return ((longitude.coerceIn(-180.0, 180.0) + 180.0) / 360.0) * worldSize
}

private fun latitudeToWorldY(
    latitude: Double,
    zoom: Double,
): Double {
    val worldSize = MAP_WORLD_TILE_SIZE * 2.0.pow(zoom)
    val clamped = latitude.coerceIn(-85.05112878, 85.05112878)
    val radians = clamped * PI / 180.0
    val normalized = (1.0 - ln(tan(radians) + 1.0 / cos(radians)) / PI) / 2.0
    return normalized * worldSize
}

private fun mercatorY(latitude: Double): Double {
    val clamped = latitude.coerceIn(-85.05112878, 85.05112878)
    val radians = clamped * PI / 180.0
    return (1.0 - ln(tan(radians) + 1.0 / cos(radians)) / PI) / 2.0
}

/**
 * Gibt den nächsten Punkt zurück, wenn er innerhalb einer zoom-adaptiven Toleranz liegt.
 * Toleranz entspricht ungefähr 30 Pixeln beim aktuellen Zoom-Level.
 */
private fun List<ActivityMapPoint>.nearestWithinTolerance(
    lat: Double,
    lng: Double,
    zoom: Double,
): ActivityMapPoint? {
    val toleranceDeg = MAP_CLICK_TOLERANCE_PX * 360.0 / (MAP_WORLD_TILE_SIZE * 2.0.pow(zoom))
    return minByOrNull { hypot(it.latitude - lat, it.longitude - lng) }
        ?.takeIf { nearest -> hypot(nearest.latitude - lat, nearest.longitude - lng) < toleranceDeg }
}
