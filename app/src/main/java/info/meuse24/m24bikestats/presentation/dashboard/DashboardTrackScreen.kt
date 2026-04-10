package info.meuse24.m24bikestats.presentation.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import info.meuse24.m24bikestats.R
import info.meuse24.m24bikestats.shared.map.GeoBounds
import info.meuse24.m24bikestats.shared.map.GeoPoint
import info.meuse24.m24bikestats.shared.map.MapViewportPadding
import info.meuse24.m24bikestats.shared.map.computeGeoBounds
import info.meuse24.m24bikestats.shared.map.estimateZoomToFit
import info.meuse24.m24bikestats.shared.map.inflateGeoBounds
import info.meuse24.m24bikestats.shared.map.inverseMercatorYNormalized
import info.meuse24.m24bikestats.shared.map.mercatorYNormalized
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow

private val TrackRouteColor = Color(0xFFD32F2F)
private val TrackStartColor = Color(0xFF2E7D32)
private val TrackEndColor = Color(0xFF6A1B9A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackScreen(
    uiState: TrackUiState,
    onLoadActivity: (String) -> Unit,
    activityId: String,
    onNavigateBack: () -> Unit,
) {
    LaunchedEffect(activityId) {
        onLoadActivity(activityId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.selectedActivityDetail?.title ?: stringResource(R.string.track_fallback_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.activity_detail_back))
                    }
                },
            )
        },
    ) { padding ->
        when {
            uiState.isActivityDetailLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.selectedActivityId == activityId && uiState.selectedActivityDetail != null -> {
                val activity = uiState.selectedActivityDetail
                val context = LocalContext.current
                val trackBounds = remember(activity.trackPoints) { calculateTrackBounds(activity.trackPoints) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    TrackMapFullScreen(
                        activity = activity,
                        trackBounds = trackBounds,
                        modifier = Modifier.fillMaxSize(),
                        onShare = { shareTrackGpx(context, activity) },
                        onShareCsv = { shareTrackCsv(context, activity, uiState.csvExportFormat) },
                        onCopyGpx = {
                            copyTrackGpxToClipboard(context, activity)
                            Toast.makeText(context, context.getString(R.string.track_gpx_copied), Toast.LENGTH_SHORT).show()
                        },
                    )
                    if (uiState.isActivityDetailRefreshing) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter),
                        )
                    }
                }
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.track_not_available))
                }
            }
        }
    }
}

@Composable
private fun TrackMapFullScreen(
    activity: ActivityDetailUiModel,
    trackBounds: GeoBounds,
    modifier: Modifier = Modifier,
    onShare: () -> Unit,
    onShareCsv: () -> Unit,
    onCopyGpx: () -> Unit,
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val trackSourceData = remember(activity.id, activity.trackPoints.size) {
        GeoJsonData.JsonString(buildTrackGeoJson(activity))
    }
    val startPointData = remember(activity.id, activity.trackPoints.size) {
        GeoJsonData.JsonString(buildSingleTrackPointGeoJson(activity.trackPoints.first(), "start"))
    }
    val endPointData = remember(activity.id, activity.trackPoints.size) {
        GeoJsonData.JsonString(buildSingleTrackPointGeoJson(activity.trackPoints.last(), "end"))
    }

    BoxWithConstraints(modifier = modifier) {
        val viewportWidth = with(density) { maxWidth.toPx().toDouble() }
        val viewportHeight = with(density) { maxHeight.toPx().toDouble() }
        val sidePaddingPx = with(density) { 40.dp.toPx().toDouble() }
        val topPaddingPx = with(density) { 92.dp.toPx().toDouble() }
        val bottomPaddingPx = with(density) { 144.dp.toPx().toDouble() }
        val autoFitPosition = remember(trackBounds, viewportWidth, viewportHeight) {
            calculateTrackCameraPosition(
                bounds = trackBounds,
                viewportWidth = viewportWidth,
                viewportHeight = viewportHeight,
                sidePaddingPx = sidePaddingPx,
                topPaddingPx = topPaddingPx,
                bottomPaddingPx = bottomPaddingPx,
            )
        }
        val cameraState = rememberCameraState(firstPosition = autoFitPosition)

        LaunchedEffect(activity.id, autoFitPosition) {
            cameraState.position = autoFitPosition
        }

        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
            cameraState = cameraState,
        ) {
            val trackSource = rememberGeoJsonSource(data = trackSourceData)
            val startPointSource = rememberGeoJsonSource(data = startPointData)
            val endPointSource = rememberGeoJsonSource(data = endPointData)
            LineLayer(
                id = "activity-track",
                source = trackSource,
                color = const(TrackRouteColor),
                width = const(5.dp),
            )
            CircleLayer(
                id = "activity-track-start",
                source = startPointSource,
                color = const(TrackStartColor),
                radius = const(7.dp),
                strokeColor = const(Color.White),
                strokeWidth = const(2.dp),
            )
            CircleLayer(
                id = "activity-track-end",
                source = endPointSource,
                color = const(TrackEndColor),
                radius = const(6.dp),
                strokeColor = const(Color.White),
                strokeWidth = const(2.dp),
            )
        }

        TrackMarkerLegend(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
        )

        TrackMapAttribution(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 16.dp, bottom = 104.dp),
        )

        TrackMapBottomBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            onShare = onShare,
            onShareCsv = onShareCsv,
            onCopyGpx = onCopyGpx,
            onAutoFit = {
                cameraState.position = autoFitPosition
                Toast.makeText(context, context.getString(R.string.track_map_fitted), Toast.LENGTH_SHORT).show()
            },
        )
    }
}

@Composable
private fun TrackMapBottomBar(
    modifier: Modifier = Modifier,
    onShare: () -> Unit,
    onShareCsv: () -> Unit,
    onCopyGpx: () -> Unit,
    onAutoFit: () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TrackMapActionButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(R.string.track_action_gpx_share_cd),
                    )
                },
                label = stringResource(R.string.track_action_share),
                onClick = onShare,
            )
            TrackMapActionButton(
                icon = {
                    Text(
                        text = stringResource(R.string.track_action_gpx_label),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                },
                label = stringResource(R.string.track_action_gpx_label),
                onClick = onCopyGpx,
            )
            TrackMapActionButton(
                icon = {
                    Text(
                        text = stringResource(R.string.track_action_csv_label),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                },
                label = stringResource(R.string.track_action_csv_label),
                onClick = onShareCsv,
            )
            TrackMapActionButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.track_action_autofit_cd),
                    )
                },
                label = stringResource(R.string.track_action_autofit),
                onClick = onAutoFit,
            )
        }
    }
}

@Composable
private fun TrackMapAttribution(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
    ) {
        Text(
            text = stringResource(R.string.track_map_attribution),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TrackMapActionButton(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FilledTonalIconButton(onClick = onClick) {
            icon()
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun TrackExportDialog(
    activity: ActivityDetailUiModel,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onCopyGpx: () -> Unit,
) {
    val metadata = remember(activity.id, activity.trackPoints.size, activity.profilePoints.size) {
        buildTrackExportMetadata(activity)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.track_export_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow(label = stringResource(R.string.track_export_label_activity), value = metadata.title)
                metadata.distanceLabel?.let { DetailRow(label = stringResource(R.string.track_export_label_distance), value = it) }
                DetailRow(label = stringResource(R.string.track_export_label_gps_points), value = metadata.trackPointCount.toString())
                DetailRow(label = stringResource(R.string.track_export_label_profile_points), value = metadata.profilePointCount.toString())
                metadata.startCoordinateLabel?.let { DetailRow(label = stringResource(R.string.track_export_label_start), value = it) }
                metadata.endCoordinateLabel?.let { DetailRow(label = stringResource(R.string.track_export_label_end), value = it) }
                DetailRow(label = stringResource(R.string.track_export_label_format), value = stringResource(R.string.track_export_format_value))
            }
        },
        confirmButton = {
            TextButton(onClick = onShare) {
                Text(stringResource(R.string.track_export_share_gpx))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onCopyGpx) {
                    Text(stringResource(R.string.track_export_copy_gpx))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.common_close))
                }
            }
        },
    )
}

@Composable
private fun TrackModeSelector(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    val tabs = listOf(
        stringResource(R.string.track_tab_map),
        stringResource(R.string.track_tab_profiles),
        stringResource(R.string.track_tab_gpx),
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            edgePadding = 0.dp,
        ) {
            tabs.forEachIndexed { index, label ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { onTabSelected(index) },
                    text = { Text(label) },
                )
            }
        }
    }
}

@Composable
private fun TrackMarkerLegend(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LegendItem(
                color = TrackStartColor,
                label = stringResource(R.string.track_export_label_start),
            )
            LegendItem(
                color = TrackEndColor,
                label = stringResource(R.string.track_export_label_end),
            )
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(10.dp)
                .height(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun GpxPreviewCard(activity: ActivityDetailUiModel) {
    val gpxText = remember(activity.id, activity.trackPoints.size) { buildTrackGpx(activity) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.track_gpx_preview_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.track_gpx_preview_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp, max = 420.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(14.dp),
                ) {
                    item {
                        Text(
                            text = gpxText,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackMapCard(activity: ActivityDetailUiModel) {
    if (activity.trackPoints.size < 2) return

    val trackBounds = remember(activity.trackPoints) { calculateTrackBounds(activity.trackPoints) }
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(longitude = trackBounds.centerLongitude, latitude = trackBounds.centerLatitude),
            zoom = estimateTrackZoom(trackBounds, viewportWidth = 360.0, viewportHeight = 320.0),
        ),
    )
    val trackSourceData = remember(activity.id, activity.trackPoints.size) {
        GeoJsonData.JsonString(buildTrackGeoJson(activity))
    }
    val startPointData = remember(activity.id, activity.trackPoints.size) {
        GeoJsonData.JsonString(buildSingleTrackPointGeoJson(activity.trackPoints.first(), "start"))
    }
    val endPointData = remember(activity.id, activity.trackPoints.size) {
        GeoJsonData.JsonString(buildSingleTrackPointGeoJson(activity.trackPoints.last(), "end"))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.track_map_tiles_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    MaplibreMap(
                        modifier = Modifier.fillMaxSize(),
                        baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
                        cameraState = cameraState,
                    ) {
                        val trackSource = rememberGeoJsonSource(data = trackSourceData)
                        val startPointSource = rememberGeoJsonSource(data = startPointData)
                        val endPointSource = rememberGeoJsonSource(data = endPointData)
                        LineLayer(
                            id = "activity-track",
                            source = trackSource,
                            color = const(TrackRouteColor),
                            width = const(5.dp),
                        )
                        CircleLayer(
                            id = "activity-track-start",
                            source = startPointSource,
                            color = const(TrackStartColor),
                            radius = const(7.dp),
                            strokeColor = const(Color.White),
                            strokeWidth = const(2.dp),
                        )
                        CircleLayer(
                            id = "activity-track-end",
                            source = endPointSource,
                            color = const(TrackEndColor),
                            radius = const(6.dp),
                            strokeColor = const(Color.White),
                            strokeWidth = const(2.dp),
                        )
                    }
                    TrackMapAttribution(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricPill(stringResource(R.string.track_map_fit_label), stringResource(R.string.track_map_fit_value))
                MetricPill(stringResource(R.string.track_export_label_start), stringResource(R.string.track_map_start_value))
                MetricPill(stringResource(R.string.track_export_label_end), stringResource(R.string.track_map_end_value))
            }
            Text(
                text = stringResource(R.string.track_map_tiles_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TrackCanvasCard(trackPoints: List<ActivityTrackPointUiModel>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = stringResource(R.string.track_canvas_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 260.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                if (trackPoints.size < 2) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.track_not_enough_gps_points))
                    }
                } else {
                    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        val projectedPoints = projectTrackPoints(trackPoints, size.width, size.height)
                        if (projectedPoints.size < 2) return@Canvas
                        val path = Path()
                        projectedPoints.forEachIndexed { index, point ->
                            if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                        }

                        drawPath(
                            path = path,
                            color = TrackRouteColor,
                            style = Stroke(width = 8f, cap = StrokeCap.Round),
                        )

                        val startOffset = Offset(projectedPoints.first().x, projectedPoints.first().y)
                        val endOffset = Offset(projectedPoints.last().x, projectedPoints.last().y)
                        drawCircle(color = TrackStartColor, radius = 12f, center = startOffset)
                        drawCircle(color = TrackEndColor, radius = 12f, center = endOffset)
                    }
                }
            }
        }
    }
}

private data class ProjectedTrackPoint(
    val x: Float,
    val y: Float,
)

private fun projectTrackPoints(
    trackPoints: List<ActivityTrackPointUiModel>,
    canvasWidth: Float,
    canvasHeight: Float,
): List<ProjectedTrackPoint> {
    if (trackPoints.size < 2) return emptyList()

    val centerLatitude = trackPoints.map { it.latitude }.average()
    val longitudeScale = cos(Math.toRadians(centerLatitude)).coerceAtLeast(0.1)
    val projected = trackPoints.map { point -> point.longitude * longitudeScale to point.latitude }

    val minX = projected.minOf { it.first }
    val maxX = projected.maxOf { it.first }
    val minY = projected.minOf { it.second }
    val maxY = projected.maxOf { it.second }
    val rawSpanX = (maxX - minX).takeIf { it > 0.0 } ?: 1e-6
    val rawSpanY = (maxY - minY).takeIf { it > 0.0 } ?: 1e-6

    val minimumAspectRatio = 0.22
    val adjustedSpanX = if (rawSpanX < rawSpanY * minimumAspectRatio) rawSpanY * minimumAspectRatio else rawSpanX
    val adjustedSpanY = if (rawSpanY < rawSpanX * minimumAspectRatio) rawSpanX * minimumAspectRatio else rawSpanY

    val contentWidth = (canvasWidth - 24f).coerceAtLeast(1f)
    val contentHeight = (canvasHeight - 24f).coerceAtLeast(1f)
    val scale = minOf(contentWidth / adjustedSpanX.toFloat(), contentHeight / adjustedSpanY.toFloat())

    val xOffset = ((canvasWidth - (adjustedSpanX.toFloat() * scale)) / 2f).coerceAtLeast(0f)
    val yOffset = ((canvasHeight - (adjustedSpanY.toFloat() * scale)) / 2f).coerceAtLeast(0f)
    val centerX = (minX + maxX) / 2.0
    val centerY = (minY + maxY) / 2.0

    return projected.map { (x, y) ->
        val normalizedX = ((x - centerX) / adjustedSpanX) + 0.5
        val normalizedY = ((y - centerY) / adjustedSpanY) + 0.5
        ProjectedTrackPoint(
            x = xOffset + (normalizedX.toFloat() * adjustedSpanX.toFloat() * scale),
            y = yOffset + ((1f - normalizedY.toFloat()) * adjustedSpanY.toFloat() * scale),
        )
    }.let { points ->
        if (points.hasVisibleVerticalVariation()) points else points.withSyntheticVerticalSpread(canvasHeight)
    }
}

private fun List<ProjectedTrackPoint>.hasVisibleVerticalVariation(): Boolean {
    if (size < 2) return false
    val minY = minOf { it.y }
    val maxY = maxOf { it.y }
    return abs(maxY - minY) >= 12f
}

private fun List<ProjectedTrackPoint>.withSyntheticVerticalSpread(canvasHeight: Float): List<ProjectedTrackPoint> {
    if (size < 2) return this
    val centerY = canvasHeight / 2f
    val visualAmplitude = (canvasHeight * 0.16f).coerceAtLeast(18f)
    return mapIndexed { index, point ->
        val progress = if (size == 1) 0f else index.toFloat() / (size - 1).toFloat()
        val wave = (progress - 0.5f) * 2f
        point.copy(y = centerY - (wave * visualAmplitude))
    }
}

private fun calculateTrackBounds(trackPoints: List<ActivityTrackPointUiModel>): GeoBounds =
    computeGeoBounds(trackPoints.map { GeoPoint(it.latitude, it.longitude) })

private fun estimateTrackZoom(
    bounds: GeoBounds,
    viewportWidth: Double,
    viewportHeight: Double,
    sidePaddingPx: Double = 28.0,
    topPaddingPx: Double = 28.0,
    bottomPaddingPx: Double = 28.0,
): Double = estimateZoomToFit(
    bounds = bounds,
    viewportWidthPx = viewportWidth,
    viewportHeightPx = viewportHeight,
    tileSize = 256.0,
    padding = MapViewportPadding(
        sidePaddingPx = sidePaddingPx,
        topPaddingPx = topPaddingPx,
        bottomPaddingPx = bottomPaddingPx,
    ),
    minCoordinateDelta = 0.0003,
    zoomAdjustment = 1.2,
    minZoom = 6.8,
    maxZoom = 15.4,
)

private fun calculateTrackCameraPosition(
    bounds: GeoBounds,
    viewportWidth: Double,
    viewportHeight: Double,
    sidePaddingPx: Double,
    topPaddingPx: Double,
    bottomPaddingPx: Double,
): CameraPosition {
    val fittedBounds = inflateGeoBounds(
        bounds = bounds,
        spanScale = 1.36,
        minLatitudeSpan = 0.0008,
    )
    val zoom = estimateTrackZoom(
        bounds = fittedBounds,
        viewportWidth = viewportWidth,
        viewportHeight = viewportHeight,
        sidePaddingPx = sidePaddingPx,
        topPaddingPx = topPaddingPx,
        bottomPaddingPx = bottomPaddingPx,
    )
    val pixelsPerWorld = 256.0 * 2.0.pow(zoom)
    val mercatorCenter = (mercatorYNormalized(fittedBounds.minLatitude) + mercatorYNormalized(fittedBounds.maxLatitude)) / 2.0
    val verticalOffsetPx = ((bottomPaddingPx - topPaddingPx) / 2.0) * 0.35
    val adjustedMercatorCenter = (mercatorCenter + (verticalOffsetPx / pixelsPerWorld)).coerceIn(0.0, 1.0)

    return CameraPosition(
        target = Position(
            longitude = fittedBounds.centerLongitude,
            latitude = inverseMercatorYNormalized(adjustedMercatorCenter),
        ),
        zoom = zoom,
    )
}

private fun buildSingleTrackPointGeoJson(
    point: ActivityTrackPointUiModel,
    kind: String,
): String = """
    {"type":"FeatureCollection","features":[{"type":"Feature","properties":{"kind":"$kind"},"geometry":{"type":"Point","coordinates":[${point.longitude},${point.latitude}]}}]}
""".trimIndent()

internal fun shareTrackGpx(
    context: Context,
    activity: ActivityDetailUiModel,
) {
    val gpxUri = createTrackGpxUri(context, activity)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/gpx+xml"
        putExtra(Intent.EXTRA_STREAM, gpxUri)
        putExtra(Intent.EXTRA_SUBJECT, activity.title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.track_share_chooser_gpx)))
}

internal fun copyTrackGpxToClipboard(
    context: Context,
    activity: ActivityDetailUiModel,
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("track.gpx", buildTrackGpx(activity)))
}

private fun shareTrackCsv(
    context: Context,
    activity: ActivityDetailUiModel,
    format: info.meuse24.m24bikestats.domain.model.CsvExportFormat,
) {
    val csvUri = createTrackCsvUri(context, activity, format)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, csvUri)
        putExtra(Intent.EXTRA_SUBJECT, "${activity.title}.csv")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.track_share_chooser_csv)))
}

@Composable
private fun ProfileChartCard(
    title: String,
    subtitle: String,
    points: List<ActivityProfilePointUiModel>,
    selector: (ActivityProfilePointUiModel) -> Double?,
    color: Color,
    valueFormatter: (Double) -> String,
) {
    val chartPoints = remember(points) {
        points.mapNotNull { point ->
            val value = selector(point) ?: return@mapNotNull null
            if (value.isNaN()) return@mapNotNull null
            point.distanceMeters to value
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                if (chartPoints.size < 2) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.track_profile_not_enough_points))
                    }
                } else {
                    val axisColor = MaterialTheme.colorScheme.outlineVariant
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 16.dp),
                    ) {
                        val leftPadding = 10f
                        val topPadding = 8f
                        val bottomPadding = 20f
                        val drawableWidth = size.width - leftPadding
                        val drawableHeight = size.height - topPadding - bottomPadding

                        val minDistance = chartPoints.minOf { it.first }
                        val maxDistance = chartPoints.maxOf { it.first }
                        val minValue = chartPoints.minOf { it.second }
                        val maxValue = chartPoints.maxOf { it.second }
                        val distanceSpan = (maxDistance - minDistance).takeIf { it > 0.0 } ?: 1.0
                        val valueSpan = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0

                        drawLine(
                            color = axisColor,
                            start = Offset(leftPadding, size.height - bottomPadding),
                            end = Offset(size.width, size.height - bottomPadding),
                            strokeWidth = 2f,
                        )
                        drawLine(
                            color = axisColor,
                            start = Offset(leftPadding, topPadding),
                            end = Offset(leftPadding, size.height - bottomPadding),
                            strokeWidth = 2f,
                        )

                        val path = Path()
                        chartPoints.forEachIndexed { index, (distance, value) ->
                            val x = leftPadding + (((distance - minDistance) / distanceSpan) * drawableWidth).toFloat()
                            val y = topPadding + (drawableHeight - (((value - minValue) / valueSpan) * drawableHeight)).toFloat()
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }

                        drawPath(
                            path = path,
                            color = color,
                            style = Stroke(width = 5f, cap = StrokeCap.Round),
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MetricPill(
                    label = stringResource(R.string.track_export_label_start),
                    value = valueFormatter(chartPoints.firstOrNull()?.second ?: 0.0),
                )
                MetricPill(
                    label = stringResource(R.string.track_profile_max_label),
                    value = valueFormatter(chartPoints.maxOfOrNull { it.second } ?: 0.0),
                )
                MetricPill(
                    label = stringResource(R.string.track_profile_end_label),
                    value = valueFormatter(chartPoints.lastOrNull()?.second ?: 0.0),
                )
            }
        }
    }
}

private fun List<ActivityProfilePointUiModel>.hasMetric(
    selector: (ActivityProfilePointUiModel) -> Double?,
): Boolean = any { point -> selector(point) != null }

private fun List<ActivityProfilePointUiModel>.metricSummary(
    selector: (ActivityProfilePointUiModel) -> Double?,
    unit: String,
): String {
    val values = mapNotNull { selector(it) }
    if (values.isEmpty()) return ""
    val average = values.average()
    val maximum = values.maxOrNull() ?: average
    return "avg ${String.format("%.1f", average)} $unit • max ${String.format("%.1f", maximum)} $unit"
}
