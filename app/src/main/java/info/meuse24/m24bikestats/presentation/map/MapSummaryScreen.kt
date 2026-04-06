package info.meuse24.m24bikestats.presentation.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.compose.runtime.snapshotFlow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow

private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

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
    val initialPosition = remember(points) {
        savedCameraPosition ?: run {
            val center = points.center()
            CameraPosition(
                target = Position(longitude = center.second, latitude = center.first),
                zoom = 5.0,
            )
        }
    }
    val cameraState = rememberCameraState(firstPosition = initialPosition)

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

private fun List<ActivityMapPoint>.center(): Pair<Double, Double> {
    val minLat = minOf { it.latitude }
    val maxLat = maxOf { it.latitude }
    val minLng = minOf { it.longitude }
    val maxLng = maxOf { it.longitude }
    return (minLat + maxLat) / 2.0 to (minLng + maxLng) / 2.0
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
    val toleranceDeg = 30.0 * 360.0 / (256.0 * 2.0.pow(zoom))
    return minByOrNull { hypot(it.latitude - lat, it.longitude - lng) }
        ?.takeIf { nearest -> hypot(nearest.latitude - lat, nearest.longitude - lng) < toleranceDeg }
}
