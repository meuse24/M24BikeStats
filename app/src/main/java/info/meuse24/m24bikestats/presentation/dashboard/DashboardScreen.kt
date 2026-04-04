package info.meuse24.m24bikestats.presentation.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import info.meuse24.m24bikestats.R
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan
import kotlin.math.PI
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

private val dashboardTabs = listOf("Aktivitäten", "Bike", "Funktionen")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onRefresh: () -> Unit,
    onLoadMoreActivities: () -> Unit,
    onActivitySearchQueryChanged: (String) -> Unit,
    onActivityDateRangeFilterChanged: (ActivityDateRangeFilter) -> Unit,
    onActivitySortOptionChanged: (ActivitySortOption) -> Unit,
    onExportActivitiesCsv: () -> Unit,
    onExportActivityDetailsCsv: () -> Unit,
    onActivitiesCsvExportHandled: () -> Unit,
    onActivityDetailsCsvExportHandled: () -> Unit,
    onNavigateToActivityDetail: (String) -> Unit,
    onNavigateToBikeDetail: (String) -> Unit,
    onLogout: () -> Unit,
    onErrorShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            onErrorShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("M24 Bike Stats")
                        Text(
                            text = dashboardTabs[selectedTabIndex],
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    if (selectedTabIndex != 2) {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Aktualisieren")
                        }
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Abmelden")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isInitialLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    ScrollableTabRow(selectedTabIndex = selectedTabIndex) {
                        dashboardTabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text(title) }
                            )
                        }
                    }

                    when (selectedTabIndex) {
                        0 -> ActivitiesScreen(
                            uiState = uiState.toActivitiesUiState(),
                            onActivitySearchQueryChanged = onActivitySearchQueryChanged,
                            onActivityDateRangeFilterChanged = onActivityDateRangeFilterChanged,
                            onActivitySortOptionChanged = onActivitySortOptionChanged,
                            onActivityClick = onNavigateToActivityDetail,
                            onActivityMapClick = {},
                            onLoadMore = onLoadMoreActivities,
                        )
                        1 -> BikeListScreen(
                            uiState = uiState.toBikeListUiState(),
                            onBikeClick = onNavigateToBikeDetail,
                        )
                        else -> FunctionsScreen(
                            uiState = uiState.toFunctionsUiState(),
                            onExportActivitiesCsv = onExportActivitiesCsv,
                            onExportActivityDetailsCsv = onExportActivityDetailsCsv,
                            onCancelActivitiesCsvExport = {},
                            onCancelActivityDetailsCsvExport = {},
                            onActivitiesCsvExportHandled = onActivitiesCsvExportHandled,
                            onActivityDetailsCsvExportHandled = onActivityDetailsCsvExportHandled,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun FunctionsOverview(
    uiState: DashboardUiState,
    onExportActivitiesCsv: () -> Unit,
    onExportActivityDetailsCsv: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            HeroCard(
                eyebrow = "Funktionen",
                title = "Datenexport",
                subtitle = "Aktivitätenlisten und Detailpunkte als teilbare CSV-Dateien exportieren.",
            ) {
                SummaryChipRow(
                    listOf(
                        "Geladen" to uiState.loadedActivityCount.toString(),
                        "Gesamt" to uiState.activityTotalCount.toString(),
                        "Sichtbar" to uiState.visibleActivityCount.toString(),
                    )
                )
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "Aktivitäten als CSV exportieren",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Die Funktion ruft alle Seiten der Aktivitätenliste ab und erstellt daraus eine teilbare CSV-Datei mit den bekannten Metriken.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    uiState.lastActivitiesCsvExport?.let { exportSummary ->
                        SectionSurface {
                            Text(
                                text = "Zuletzt exportiert",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            OptionalRow("Datei", exportSummary.fileName)
                            OptionalRow("Aktivitäten", exportSummary.activityCount.toString())
                            OptionalRow("Zeitpunkt", exportSummary.exportedAtLabel)
                        }
                    }
                    if (uiState.isExportingActivitiesCsv) {
                        val totalCount = uiState.exportTotalActivityCount
                        val loadedCount = uiState.exportLoadedActivityCount
                        val progress = if (totalCount > 0) {
                            loadedCount.toFloat() / totalCount.toFloat()
                        } else {
                            0f
                        }

                        Text(
                            text = "$loadedCount von $totalCount Aktivitäten geladen",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Button(
                        onClick = onExportActivitiesCsv,
                        enabled = !uiState.isExportingActivitiesCsv &&
                            !uiState.isExportingActivityDetailsCsv &&
                            !uiState.isInitialLoading &&
                            !uiState.isRefreshing,
                    ) {
                        if (uiState.isExportingActivitiesCsv) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height(18.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Export läuft")
                        } else {
                            Text("CSV exportieren")
                        }
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "Sichtbare Detailpunkte als CSV exportieren",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Verwendet den aktuell sichtbaren Aktivitätssatz und exportiert alle Detailpunkte aus Cache und bei Bedarf per Live-Nachladen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SectionSurface {
                        OptionalRow("Sichtbare Aktivitäten", uiState.visibleActivityCount.toString())
                        OptionalRow("Geladene Aktivitäten", uiState.loadedActivityCount.toString())
                    }
                    uiState.lastActivityDetailsCsvExport?.let { exportSummary ->
                        SectionSurface {
                            Text(
                                text = "Zuletzt exportiert",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            OptionalRow("Datei", exportSummary.fileName)
                            OptionalRow("Aktivitäten", exportSummary.activityCount.toString())
                            OptionalRow("Detailpunkte", exportSummary.detailPointCount.toString())
                            OptionalRow("Zeitpunkt", exportSummary.exportedAtLabel)
                        }
                    }
                    if (uiState.isExportingActivityDetailsCsv) {
                        val totalCount = uiState.exportDetailedTotalActivityCount
                        val loadedCount = uiState.exportDetailedLoadedActivityCount
                        val progress = if (totalCount > 0) {
                            loadedCount.toFloat() / totalCount.toFloat()
                        } else {
                            0f
                        }

                        Text(
                            text = "$loadedCount von $totalCount Aktivitäten geladen",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Button(
                        onClick = onExportActivityDetailsCsv,
                        enabled = uiState.visibleActivityCount > 0 &&
                            !uiState.isExportingActivitiesCsv &&
                            !uiState.isExportingActivityDetailsCsv &&
                            !uiState.isInitialLoading &&
                            !uiState.isRefreshing,
                    ) {
                        if (uiState.isExportingActivityDetailsCsv) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height(18.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Detail-Export läuft")
                        } else {
                            Text("Detail-CSV exportieren")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    uiState: ActivityDetailScreenUiState,
    onLoadActivity: (String) -> Unit,
    onRefreshActivity: (String) -> Unit,
    activityId: String,
    onNavigateToTrack: (String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val selectedActivity = uiState.selectedActivityDetail?.takeIf { uiState.selectedActivityId == activityId }

    LaunchedEffect(activityId) {
        onLoadActivity(activityId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedActivity?.title ?: stringResource(R.string.activity_detail_fallback_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.activity_detail_back))
                    }
                },
                actions = {
                    selectedActivity?.let { activity ->
                        IconButton(onClick = { shareActivityDetail(context, activity) }) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = stringResource(R.string.activity_detail_share),
                            )
                        }
                    }
                    IconButton(onClick = { onRefreshActivity(activityId) }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.activity_detail_refresh))
                    }
                }
            )
        }
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (uiState.isActivityDetailRefreshing) {
                        item {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    items(activity.sections) { section ->
                        DetailSectionCard(
                            section = section,
                            activity = activity,
                            onNavigateToTrack = onNavigateToTrack,
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
                    Text(stringResource(R.string.activity_detail_not_found))
                }
            }
        }
    }
}

private fun shareActivityDetail(
    context: Context,
    activity: ActivityDetailUiModel,
) {
    val shareText = buildString {
        appendLine(activity.title)
        activity.subtitle?.takeIf { it.isNotBlank() }?.let {
            appendLine(it)
        }

        activity.sections.forEach { section ->
            appendLine()
            appendLine(section.title)
            section.rows.forEach { (label, value) ->
                appendLine("$label: $value")
            }
        }
    }.trim()

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, activity.title)
        putExtra(Intent.EXTRA_TEXT, shareText)
    }

    context.startActivity(
        Intent.createChooser(
            shareIntent,
            context.getString(R.string.activity_detail_share_chooser),
        )
    )
}

internal fun shareBikeDetail(
    context: Context,
    bike: BikeCardUiModel,
) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, bike.title)
        putExtra(Intent.EXTRA_TEXT, bike.shareText)
    }

    context.startActivity(
        Intent.createChooser(
            shareIntent,
            context.getString(R.string.dashboard_bike_share_chooser),
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackScreen(
    uiState: TrackUiState,
    onLoadActivity: (String) -> Unit,
    onRefreshActivity: (String) -> Unit,
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
                actions = {
                    IconButton(onClick = { onRefreshActivity(activityId) }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.track_refresh))
                    }
                }
            )
        }
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
    trackBounds: TrackBounds,
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
                color = const(MaterialTheme.colorScheme.primary),
                width = const(5.dp),
            )
            CircleLayer(
                id = "activity-track-start",
                source = startPointSource,
                color = const(MaterialTheme.colorScheme.secondary),
                radius = const(7.dp),
                strokeColor = const(Color.White),
                strokeWidth = const(2.dp),
            )
            CircleLayer(
                id = "activity-track-end",
                source = endPointSource,
                color = const(MaterialTheme.colorScheme.tertiary),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BikeDetailScreen(
    uiState: BikeDetailScreenUiState,
    onLoadBike: (String) -> Unit,
    onRefreshBike: (String) -> Unit,
    bikeId: String,
    onNavigateBack: () -> Unit,
) {
    LaunchedEffect(bikeId) {
        onLoadBike(bikeId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.selectedBikeDetail?.title ?: stringResource(R.string.bike_detail_fallback_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.activity_detail_back))
                    }
                },
                actions = {
                    IconButton(onClick = { onRefreshBike(bikeId) }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.bike_detail_refresh))
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isBikeDetailLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.selectedBikeId == bikeId && uiState.selectedBikeDetail != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (uiState.isBikeDetailRefreshing) {
                        item {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    item {
                        HeroCard(
                            eyebrow = stringResource(R.string.bike_detail_eyebrow),
                            title = uiState.selectedBikeDetail.title,
                            subtitle = uiState.selectedBikeDetail.subtitle ?: stringResource(R.string.bike_detail_subtitle),
                        )
                    }
                    items(uiState.selectedBikeDetail.sections) { section ->
                        DetailSectionCard(section)
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
                    Text(stringResource(R.string.bike_detail_not_available))
                }
            }
        }
    }
}

@Composable
internal fun ActivitiesOverview(
    uiState: DashboardUiState,
    activities: List<ActivityCardUiModel>,
    onActivitySearchQueryChanged: (String) -> Unit,
    onActivityDateRangeFilterChanged: (ActivityDateRangeFilter) -> Unit,
    onActivitySortOptionChanged: (ActivitySortOption) -> Unit,
    onActivityClick: (String) -> Unit,
    onLoadMore: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HeroCard(
                eyebrow = "Tourenübersicht",
                title = "${uiState.visibleActivityCount} sichtbar • ${uiState.loadedActivityCount} geladen",
                subtitle = "Die App liest aktuell die bestätigten Summary-Daten aus Bosch Smart System.",
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        MetricPill(
                            label = "Sichtbar",
                            value = uiState.visibleActivityCount.toString(),
                        )
                    }
                    item {
                        MetricPill(
                            label = "Geladen",
                            value = uiState.loadedActivityCount.toString(),
                        )
                    }
                    item {
                        MetricPill(
                            label = "Gesamt",
                            value = uiState.activityTotalCount.toString(),
                        )
                    }
                    item {
                        MetricPill(
                            label = "Status",
                            value = if (uiState.isRefreshing) "Aktualisiert..." else "Bereit",
                        )
                    }
                }
            }
        }

        item {
            ActivityFilterSection(
                searchQuery = uiState.activitySearchQuery,
                onSearchQueryChanged = onActivitySearchQueryChanged,
                selectedDateRange = uiState.activityDateRangeFilter,
                selectedSortOption = uiState.activitySortOption,
                onDateRangeSelected = onActivityDateRangeFilterChanged,
                onSortOptionSelected = onActivitySortOptionChanged,
            )
        }

        if (activities.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Keine Aktivitäten für diese Auswahl",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Passe Datumsbereich oder Sortierung an, um andere Touren anzuzeigen.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        items(activities, key = { it.id }) { activity ->
            ActivityCard(
                activity = activity,
                onClick = { onActivityClick(activity.id) },
            )
        }

        item {
            when {
                uiState.isLoadingMoreActivities -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.canLoadMoreActivities -> {
                    OutlinedButton(
                        onClick = onLoadMore,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Mehr Aktivitäten laden")
                    }
                }
            }
        }
    }
}

@Composable
internal fun ActivityFilterSection(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    selectedDateRange: ActivityDateRangeFilter,
    selectedSortOption: ActivitySortOption,
    onDateRangeSelected: (ActivityDateRangeFilter) -> Unit,
    onSortOptionSelected: (ActivitySortOption) -> Unit,
) {
    var isSearchVisible by rememberSaveable { mutableStateOf(searchQuery.isNotBlank()) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            isSearchVisible = true
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.filter_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                FilledTonalIconButton(
                    onClick = {
                        if (isSearchVisible && searchQuery.isBlank()) {
                            isSearchVisible = false
                        } else {
                            isSearchVisible = true
                        }
                    },
                ) {
                    Icon(
                        imageVector = if (isSearchVisible && searchQuery.isBlank()) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (isSearchVisible && searchQuery.isBlank()) {
                            stringResource(R.string.filter_search_clear)
                        } else {
                            stringResource(R.string.filter_search_label)
                        },
                    )
                }
            }

            if (isSearchVisible || searchQuery.isNotBlank()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.filter_search_placeholder)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = if (searchQuery.isNotBlank()) {
                        {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.filter_search_clear))
                            }
                        }
                    } else {
                        null
                    },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CompactFilterDropdown(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.filter_date_range),
                    selectedLabel = stringResource(selectedDateRange.labelRes),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                        )
                    },
                ) { dismiss ->
                    ActivityDateRangeFilter.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(stringResource(option.labelRes)) },
                            onClick = {
                                onDateRangeSelected(option)
                                dismiss()
                            },
                        )
                    }
                }

                CompactFilterDropdown(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.filter_sorting),
                    selectedLabel = stringResource(selectedSortOption.labelRes),
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = null,
                        )
                    },
                ) { dismiss ->
                    ActivitySortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(stringResource(option.labelRes)) },
                            onClick = {
                                onSortOptionSelected(option)
                                dismiss()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactFilterDropdown(
    label: String,
    selectedLabel: String,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    content: @Composable ((dismiss: () -> Unit) -> Unit),
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        ) {
            icon()
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            content { expanded = false }
        }
    }
}

@Composable
internal fun BikesOverview(
    bikes: List<BikeCardUiModel>,
    isRefreshing: Boolean,
    onBikeClick: (String) -> Unit,
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HeroCard(
                eyebrow = "Bike-Profil",
                title = if (bikes.isEmpty()) "Kein Bike gefunden" else "${bikes.size} Bike${if (bikes.size == 1) "" else "s"} verfügbar",
                subtitle = if (isRefreshing) "Bike-Daten werden aktualisiert." else "Komponenten-, Assist- und Batterieinformationen aus Bosch Smart System.",
            )
        }

        items(bikes, key = { it.id }) { bike ->
            BikeOverviewCard(
                bike = bike,
                onClick = { onBikeClick(bike.id) },
                onShareClick = { shareBikeDetail(context, bike) },
            )
        }
    }
}

@Composable
internal fun HeroCard(
    eyebrow: String,
    title: String,
    subtitle: String,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable (() -> Unit)? = null,
) {
    val colorScheme = MaterialTheme.colorScheme
    val textAlign = if (horizontalAlignment == Alignment.CenterHorizontally) {
        TextAlign.Center
    } else {
        TextAlign.Start
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            colorScheme.primaryContainer,
                            colorScheme.secondaryContainer,
                        )
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = horizontalAlignment,
        ) {
            if (eyebrow.isNotBlank()) {
                Text(
                    text = eyebrow,
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.onPrimaryContainer,
                    textAlign = textAlign,
                )
            }
            if (title.isNotBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = textAlign,
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                textAlign = textAlign,
            )
            content?.invoke()
        }
    }
}

@Composable
internal fun ActivityCard(
    activity: ActivityCardUiModel,
    onClick: () -> Unit,
    onMapClick: (() -> Unit)? = null,
    onShareClick: (() -> Unit)? = null,
    primaryActionLabel: String? = null,
) {
    val detailActionLabel = primaryActionLabel ?: stringResource(R.string.dashboard_activity_detail_button)
    val metricSummary = buildList {
        add(stringResource(R.string.dashboard_card_speed) to activity.speedLabel.withLineBreakBeforeMax())
        activity.powerLabel?.let { add(stringResource(R.string.dashboard_card_power) to it.withLineBreakBeforeMax()) }
        activity.elevationLabel?.let { add(stringResource(R.string.dashboard_label_elevation) to it) }
        activity.caloriesLabel?.let { add(stringResource(R.string.dashboard_label_calories) to it) }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = activity.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = activity.dateLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    StatusBadge(activity.durationLabel)
                    StatusBadge(activity.distanceLabel)
                }
            }

            ActivityMetricGrid(
                summary = metricSummary,
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ActivityCardActionButton(
                        label = detailActionLabel,
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        onClick = onClick,
                        modifier = Modifier.weight(1f),
                    )
                    onMapClick?.let { navigateToMap ->
                        ActivityCardActionButton(
                            label = stringResource(R.string.dashboard_activity_map_button),
                            icon = Icons.Default.Map,
                            onClick = navigateToMap,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    onShareClick?.let { share ->
                        ActivityCardActionButton(
                            label = stringResource(R.string.dashboard_activity_share_button),
                            icon = Icons.Default.Share,
                            onClick = share,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityMetricGrid(
    summary: List<Pair<String, String>>,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        summary.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowItems.forEach { (label, value) ->
                    ActivityMetricTile(
                        label = label,
                        value = value,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ActivityMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ActivityCardActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun String.withLineBreakBeforeMax(): String =
    replace(" • max ", "\nmax ")
        .replace(", max ", "\nmax ")

@Composable
internal fun BikeOverviewCard(
    bike: BikeCardUiModel,
    onClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = bike.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                bike.subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            SummaryChipRow(
                summary = buildList {
                    bike.odometerLabel?.let { add(stringResource(R.string.dashboard_card_odometer) to it) }
                    bike.assistSpeedLabel?.let { add(stringResource(R.string.dashboard_card_assist) to it) }
                    bike.walkAssistLabel?.let { add(stringResource(R.string.dashboard_card_walk_assist) to it) }
                },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                itemContent = { label, value ->
                    CompactMetricPill(label = label, value = value)
                },
            )
            bike.batterySummary?.let { batterySummary ->
                BikeInfoSurface(
                    label = stringResource(R.string.dashboard_battery_fallback_title),
                    value = batterySummary,
                )
            }
            bike.powerOnSummary?.let { powerOnSummary ->
                BikeInfoSurface(
                    label = stringResource(R.string.dashboard_card_usage),
                    value = powerOnSummary,
                )
            }
            bike.assistModesSummary?.let { assistModesSummary ->
                BikeInfoSurface(
                    label = stringResource(R.string.dashboard_card_assist_ranges),
                    value = assistModesSummary,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BikeCardActionButton(
                    label = stringResource(R.string.dashboard_bike_share_button),
                    icon = Icons.Default.Share,
                    onClick = onShareClick,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                BikeCardActionButton(
                    label = stringResource(R.string.dashboard_bike_detail_button),
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    onClick = onClick,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun BikeCardActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BikeInfoSurface(
    label: String,
    value: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun DetailSectionCard(
    section: DetailSectionUiModel,
    activity: ActivityDetailUiModel? = null,
    onNavigateToTrack: (String) -> Unit = {},
) {
    var showExportDialog by rememberSaveable(activity?.id, section.title) { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                section.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            section.rows.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowItems.forEach { (label, value) ->
                        DetailValueTile(
                            label = label,
                            value = value,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            if (section.actions.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    section.actions.forEach { action ->
                        ActivityCardActionButton(
                            label = action.label,
                            icon = when (action.type) {
                                DetailSectionActionType.SHARE -> Icons.Default.Share
                                DetailSectionActionType.MAP -> Icons.Default.Map
                            },
                            onClick = {
                                when (action.type) {
                                    DetailSectionActionType.SHARE -> {
                                        if (activity != null) showExportDialog = true
                                    }
                                    DetailSectionActionType.MAP -> {
                                        activity?.let { onNavigateToTrack(it.id) }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
    if (showExportDialog && activity != null) {
        TrackExportDialog(
            activity = activity,
            onDismiss = { showExportDialog = false },
            onShare = {
                shareTrackGpx(context, activity)
            },
            onCopyGpx = {
                copyTrackGpxToClipboard(context, activity)
                Toast.makeText(context, context.getString(R.string.track_gpx_copied), Toast.LENGTH_SHORT).show()
            },
        )
    }
}

@Composable
private fun DetailValueTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
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
                color = MaterialTheme.colorScheme.secondary,
                label = stringResource(R.string.track_export_label_start),
            )
            LegendItem(
                color = MaterialTheme.colorScheme.tertiary,
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
private fun TrackExportDialog(
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
        )
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
                        color = const(MaterialTheme.colorScheme.primary),
                        width = const(5.dp),
                    )
                    CircleLayer(
                        id = "activity-track-start",
                        source = startPointSource,
                        color = const(MaterialTheme.colorScheme.secondary),
                        radius = const(7.dp),
                        strokeColor = const(Color.White),
                        strokeWidth = const(2.dp),
                    )
                    CircleLayer(
                        id = "activity-track-end",
                        source = endPointSource,
                        color = const(MaterialTheme.colorScheme.tertiary),
                        radius = const(6.dp),
                        strokeColor = const(Color.White),
                        strokeWidth = const(2.dp),
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
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

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
                            color = primaryColor,
                            style = Stroke(width = 8f, cap = StrokeCap.Round)
                        )

                        val startOffset = Offset(projectedPoints.first().x, projectedPoints.first().y)
                        val endOffset = Offset(projectedPoints.last().x, projectedPoints.last().y)
                        drawCircle(
                            color = secondaryColor,
                            radius = 12f,
                            center = startOffset
                        )
                        drawCircle(
                            color = tertiaryColor,
                            radius = 12f,
                            center = endOffset
                        )
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

    val projected = trackPoints.map { point ->
        point.longitude * longitudeScale to point.latitude
    }

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
    val scale = minOf(
        contentWidth / adjustedSpanX.toFloat(),
        contentHeight / adjustedSpanY.toFloat(),
    )

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
        if (points.hasVisibleVerticalVariation()) points
        else points.withSyntheticVerticalSpread(canvasHeight)
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

private data class TrackBounds(
    val minLatitude: Double,
    val maxLatitude: Double,
    val minLongitude: Double,
    val maxLongitude: Double,
    val centerLatitude: Double,
    val centerLongitude: Double,
)

private fun calculateTrackBounds(trackPoints: List<ActivityTrackPointUiModel>): TrackBounds {
    val minLatitude = trackPoints.minOf { it.latitude }
    val maxLatitude = trackPoints.maxOf { it.latitude }
    val minLongitude = trackPoints.minOf { it.longitude }
    val maxLongitude = trackPoints.maxOf { it.longitude }
    return TrackBounds(
        minLatitude = minLatitude,
        maxLatitude = maxLatitude,
        minLongitude = minLongitude,
        maxLongitude = maxLongitude,
        centerLatitude = (minLatitude + maxLatitude) / 2.0,
        centerLongitude = (minLongitude + maxLongitude) / 2.0,
    )
}

private fun inflateTrackBounds(bounds: TrackBounds): TrackBounds {
    val latitudeSpan = (bounds.maxLatitude - bounds.minLatitude).coerceAtLeast(0.0008)
    val longitudeSpan = (bounds.maxLongitude - bounds.minLongitude).coerceAtLeast(0.0008)
    val latitudePadding = latitudeSpan * 0.18
    val longitudePadding = longitudeSpan * 0.18

    val minLatitude = (bounds.minLatitude - latitudePadding).coerceIn(-85.05112878, 85.05112878)
    val maxLatitude = (bounds.maxLatitude + latitudePadding).coerceIn(-85.05112878, 85.05112878)
    val minLongitude = (bounds.minLongitude - longitudePadding).coerceIn(-180.0, 180.0)
    val maxLongitude = (bounds.maxLongitude + longitudePadding).coerceIn(-180.0, 180.0)

    return TrackBounds(
        minLatitude = minLatitude,
        maxLatitude = maxLatitude,
        minLongitude = minLongitude,
        maxLongitude = maxLongitude,
        centerLatitude = (minLatitude + maxLatitude) / 2.0,
        centerLongitude = (minLongitude + maxLongitude) / 2.0,
    )
}

private fun estimateTrackZoom(
    bounds: TrackBounds,
    viewportWidth: Double,
    viewportHeight: Double,
    sidePaddingPx: Double = 28.0,
    topPaddingPx: Double = 28.0,
    bottomPaddingPx: Double = 28.0,
): Double {
    val usableWidth = (viewportWidth - (sidePaddingPx * 2.0)).coerceAtLeast(1.0)
    val usableHeight = (viewportHeight - topPaddingPx - bottomPaddingPx).coerceAtLeast(1.0)
    val longitudeDelta = (bounds.maxLongitude - bounds.minLongitude).coerceAtLeast(0.0003)
    val latitudeFraction = abs(mercatorY(bounds.maxLatitude) - mercatorY(bounds.minLatitude)).coerceAtLeast(0.0003)
    val longitudeZoom = ln(usableWidth * 360.0 / (longitudeDelta * 256.0)) / ln(2.0)
    val latitudeZoom = ln(usableHeight / (latitudeFraction * 256.0)) / ln(2.0)
    return (minOf(longitudeZoom, latitudeZoom) - 1.2).coerceIn(6.8, 15.4)
}

private fun calculateTrackCameraPosition(
    bounds: TrackBounds,
    viewportWidth: Double,
    viewportHeight: Double,
    sidePaddingPx: Double,
    topPaddingPx: Double,
    bottomPaddingPx: Double,
): CameraPosition {
    val fittedBounds = inflateTrackBounds(bounds)
    val zoom = estimateTrackZoom(
        bounds = fittedBounds,
        viewportWidth = viewportWidth,
        viewportHeight = viewportHeight,
        sidePaddingPx = sidePaddingPx,
        topPaddingPx = topPaddingPx,
        bottomPaddingPx = bottomPaddingPx,
    )
    val pixelsPerWorld = 256.0 * 2.0.pow(zoom)
    val mercatorCenter = (mercatorY(fittedBounds.minLatitude) + mercatorY(fittedBounds.maxLatitude)) / 2.0
    // Apply only a partial center shift; the larger bottom bar should not push the whole
    // track into the top edge of the visible map.
    val verticalOffsetPx = ((bottomPaddingPx - topPaddingPx) / 2.0) * 0.35
    val adjustedMercatorCenter = (mercatorCenter + (verticalOffsetPx / pixelsPerWorld))
        .coerceIn(0.0, 1.0)

    return CameraPosition(
        target = Position(
            longitude = fittedBounds.centerLongitude,
            latitude = inverseMercatorY(adjustedMercatorCenter),
        ),
        zoom = zoom,
    )
}

private fun mercatorY(latitude: Double): Double {
    val clamped = latitude.coerceIn(-85.05112878, 85.05112878)
    val radians = clamped * PI / 180.0
    return (1.0 - ln(tan(radians) + 1.0 / cos(radians)) / PI) / 2.0
}

private fun inverseMercatorY(mercatorY: Double): Double {
    val normalized = mercatorY.coerceIn(0.0, 1.0)
    val n = PI * (1.0 - 2.0 * normalized)
    return Math.toDegrees(kotlin.math.atan(kotlin.math.sinh(n)))
}

private fun buildSingleTrackPointGeoJson(
    point: ActivityTrackPointUiModel,
    kind: String,
): String = """
    {"type":"FeatureCollection","features":[{"type":"Feature","properties":{"kind":"$kind"},"geometry":{"type":"Point","coordinates":[${point.longitude},${point.latitude}]}}]}
""".trimIndent()

private fun shareTrackGpx(
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

private fun copyTrackGpxToClipboard(
    context: Context,
    activity: ActivityDetailUiModel,
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(
        ClipData.newPlainText("track.gpx", buildTrackGpx(activity))
    )
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
    color: androidx.compose.ui.graphics.Color,
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
                            .padding(horizontal = 12.dp, vertical = 16.dp)
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
                            if (index == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
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

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
internal fun SummaryChipRow(
    summary: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
    itemContent: @Composable (label: String, value: String) -> Unit = { label, value ->
        MetricPill(label = label, value = value)
    },
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
    ) {
        items(summary) { (label, value) ->
            itemContent(label, value)
        }
    }
}

@Composable
internal fun MetricPill(
    label: String,
    value: String,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun CompactMetricPill(
    label: String,
    value: String,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun StatusBadge(value: String) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
internal fun SectionSurface(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            content()
        }
    }
}

@Composable
internal fun OptionalRow(
    label: String,
    value: String?,
) {
    if (value == null) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
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
