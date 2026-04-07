package info.meuse24.m24bikestats.presentation.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import info.meuse24.m24bikestats.R
import info.meuse24.m24bikestats.domain.model.ActivityMapPoint
import info.meuse24.m24bikestats.shared.map.BoundsCenterStrategy
import info.meuse24.m24bikestats.shared.map.GeoPoint
import info.meuse24.m24bikestats.shared.map.MapViewportPadding
import info.meuse24.m24bikestats.shared.map.computeGeoBounds
import info.meuse24.m24bikestats.shared.map.estimateZoomToFit
import info.meuse24.m24bikestats.shared.map.fitZoomToPoints
import info.meuse24.m24bikestats.shared.map.inflateGeoBounds
import info.meuse24.m24bikestats.shared.map.pointsFitInViewport
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
import kotlin.math.hypot
import kotlin.math.pow

private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val MAP_WORLD_TILE_SIZE = 512.0
private const val MAP_CLICK_TOLERANCE_PX = 30.0
private const val MAP_FIT_MARGIN_FRACTION = 0.10
private const val MAP_MIN_ZOOM = 2.5
private const val MAP_MAX_ZOOM = 14.0

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
        val pointsFingerprint = remember(points) { points.toFingerprint() }
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
        var hasAppliedAutoFitForDataset by remember(pointsFingerprint) { mutableStateOf(false) }

        // Re-apply auto-fit only for new datasets. Viewport changes alone should not override
        // an explicit user pan/zoom with a forced reset.
        LaunchedEffect(pointsFingerprint, useSavedCamera, autoFitPosition) {
            if (!useSavedCamera && !hasAppliedAutoFitForDataset) {
                cameraState.position = autoFitPosition
                hasAppliedAutoFitForDataset = true
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

private fun calculateMapCameraPosition(
    points: List<ActivityMapPoint>,
    viewportWidthPx: Double,
    viewportHeightPx: Double,
): CameraPosition {
    val bounds = inflateGeoBounds(
        bounds = computeGeoBounds(
            points = points.map { GeoPoint(it.latitude, it.longitude) },
            centerStrategy = BoundsCenterStrategy.AVERAGE_POINTS,
        ),
        spanScale = 1.22,
        minLatitudeSpan = 0.02,
    )
    val zoom = estimateZoomToFit(
        bounds = bounds,
        viewportWidthPx = viewportWidthPx,
        viewportHeightPx = viewportHeightPx,
        tileSize = MAP_WORLD_TILE_SIZE,
        padding = MapViewportPadding(
            sidePaddingPx = 36.0,
            topPaddingPx = 36.0,
            bottomPaddingPx = 36.0,
        ),
        minCoordinateDelta = 0.0003,
        zoomAdjustment = 1.05,
        minZoom = 3.2,
        maxZoom = MAP_MAX_ZOOM,
    )
    val fittedZoom = fitZoomToPoints(
        points = points.map { GeoPoint(it.latitude, it.longitude) },
        centerLatitude = bounds.centerLatitude,
        centerLongitude = bounds.centerLongitude,
        initialZoom = zoom,
        minZoom = MAP_MIN_ZOOM,
        maxZoom = MAP_MAX_ZOOM,
        viewportWidthPx = viewportWidthPx,
        viewportHeightPx = viewportHeightPx,
        marginFraction = MAP_FIT_MARGIN_FRACTION,
        tileSize = MAP_WORLD_TILE_SIZE,
    )
    return CameraPosition(
        target = Position(
            longitude = bounds.centerLongitude,
            latitude = bounds.centerLatitude,
        ),
        zoom = fittedZoom,
    )
}

private fun shouldUseSavedCameraPosition(
    saved: CameraPosition,
    autoFit: CameraPosition,
    points: List<ActivityMapPoint>,
    viewportWidthPx: Double,
    viewportHeightPx: Double,
): Boolean {
    // Asymmetric tolerance is intentional:
    // a slightly wider zoomed-out saved view is still usable, while too zoomed-in views often hide points.
    val zoomMatches = saved.zoom in (autoFit.zoom - 3.0)..(autoFit.zoom + 1.0)
    if (!zoomMatches) return false
    return pointsFitInViewport(
        points = points.map { GeoPoint(it.latitude, it.longitude) },
        centerLatitude = saved.target.latitude,
        centerLongitude = saved.target.longitude,
        zoom = saved.zoom,
        viewportWidthPx = viewportWidthPx,
        viewportHeightPx = viewportHeightPx,
        marginFraction = MAP_FIT_MARGIN_FRACTION,
        tileSize = MAP_WORLD_TILE_SIZE,
    )
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

private fun List<ActivityMapPoint>.toFingerprint(): Int =
    fold(1) { acc, point ->
        var value = 31 * acc + point.activityId.hashCode()
        value = 31 * value + point.latitude.hashCode()
        value = 31 * value + point.longitude.hashCode()
        value
    }
