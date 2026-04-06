package info.meuse24.m24bikestats.presentation.statistics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberEnd
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.component.shapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.core.cartesian.FadingEdges
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.common.Insets
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import com.patrykandpatrick.vico.core.common.shape.DashedShape
import com.patrykandpatrick.vico.core.cartesian.axis.Axis
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarkerController
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarkerVisibilityListener
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.core.common.Position
import info.meuse24.m24bikestats.R
import kotlin.math.roundToInt

private const val STATISTICS_SCROLL_THRESHOLD = 14
private const val STATISTICS_ZOOM_THRESHOLD = 20
private const val STATISTICS_MAX_ZOOM = 2f
private const val STATISTICS_COLUMN_WIDTH_DP = 8f
private const val STATISTICS_COLUMN_COLLECTION_SPACING_DP = 8f
private const val STATISTICS_BOTTOM_AXIS_ROTATION_THRESHOLD = 12

private val statisticsTourCountsKey = ExtraStore.Key<List<Int>>()

@Composable
fun StatisticsScreen(
    uiState: StatisticsUiState,
    onGroupingSelected: (StatisticsGrouping) -> Unit,
    onPeriodSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.isLoading) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            StatisticsSummaryRow(uiState = uiState)
        }
        item {
            GroupingSelector(
                selectedGrouping = uiState.grouping,
                onGroupingSelected = onGroupingSelected,
            )
        }
        item {
            StatisticsChartCard(
                periods = uiState.periods,
                onPeriodSelected = onPeriodSelected,
            )
        }
        item {
            AnimatedVisibility(visible = uiState.selectedPeriod != null) {
                uiState.selectedPeriod?.let { StatisticsDetailCard(period = it) }
            }
        }
    }
}

@Composable
private fun StatisticsSummaryRow(
    uiState: StatisticsUiState,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatisticsMetricTile(
                label = stringResource(R.string.statistics_summary_tours),
                value = uiState.totalTours.toString(),
                modifier = Modifier.weight(1f),
            )
            StatisticsMetricTile(
                label = stringResource(R.string.statistics_summary_distance),
                value = uiState.totalDistanceKm.toReadableDistance(),
                modifier = Modifier.weight(1f),
            )
            StatisticsMetricTile(
                label = stringResource(R.string.statistics_summary_duration),
                value = uiState.totalDurationHours.toReadableHours(),
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatisticsMetricTile(
                label = stringResource(R.string.statistics_summary_avg_distance),
                value = uiState.avgDistanceKm.toReadableDistance(),
                modifier = Modifier.weight(1f),
            )
            StatisticsMetricTile(
                label = stringResource(R.string.statistics_summary_avg_duration),
                value = uiState.avgDurationHours.toReadableHours(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GroupingSelector(
    selectedGrouping: StatisticsGrouping,
    onGroupingSelected: (StatisticsGrouping) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilterChip(
            selected = selectedGrouping == StatisticsGrouping.WEEK,
            onClick = { onGroupingSelected(StatisticsGrouping.WEEK) },
            label = { Text(stringResource(R.string.statistics_group_week)) },
        )
        FilterChip(
            selected = selectedGrouping == StatisticsGrouping.MONTH,
            onClick = { onGroupingSelected(StatisticsGrouping.MONTH) },
            label = { Text(stringResource(R.string.statistics_group_month)) },
        )
    }
}

@Composable
private fun StatisticsChartCard(
    periods: List<PeriodStats>,
    onPeriodSelected: (Long) -> Unit,
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val distanceColor = MaterialTheme.colorScheme.primary
    val durationColor = MaterialTheme.colorScheme.tertiary
    // Vico's column data-label formatter receives only the plotted Y value, not the bar index.
    val chartDistanceValues = remember(periods) { periods.toChartDistanceValues() }
    val chartDistanceValueIndices = remember(chartDistanceValues) {
        chartDistanceValues.withIndex().associate { it.value to it.index }
    }
    val marker = rememberStatisticsMarker(
        periods = periods,
        distanceColor = distanceColor,
    )
    val markerVisibilityListener = remember(periods, onPeriodSelected) {
        object : CartesianMarkerVisibilityListener {
            override fun onShown(marker: CartesianMarker, targets: List<CartesianMarker.Target>) {
                targets.updateSelection(periods, onPeriodSelected)
            }

            override fun onUpdated(marker: CartesianMarker, targets: List<CartesianMarker.Target>) {
                targets.updateSelection(periods, onPeriodSelected)
            }

            override fun onHidden(marker: CartesianMarker) {
                // Keep the selected detail card visible until a new period is tapped or the grouping changes.
            }
        }
    }
    val scrollThreshold = STATISTICS_SCROLL_THRESHOLD
    val zoomThreshold = STATISTICS_ZOOM_THRESHOLD
    val scrollState = rememberVicoScrollState(
        scrollEnabled = periods.size > scrollThreshold,
    )
    val zoomEnabled = periods.size >= zoomThreshold
    val minimumZoom = remember(zoomEnabled) {
        if (zoomEnabled) {
            Zoom.max(Zoom.fixed(), Zoom.Content)
        } else {
            Zoom.Content
        }
    }
    val maximumZoom = remember(zoomEnabled) {
        if (zoomEnabled) {
            Zoom.max(Zoom.fixed(STATISTICS_MAX_ZOOM), Zoom.Content)
        } else {
            Zoom.Content
        }
    }
    val zoomState = rememberVicoZoomState(
        zoomEnabled = zoomEnabled,
        initialZoom = minimumZoom,
        minZoom = minimumZoom,
        maxZoom = maximumZoom,
    )
    val markerController = remember { CartesianMarkerController.toggleOnTap() }
    val fadingEdges = remember(periods.size) {
        if (periods.size > scrollThreshold) {
            FadingEdges(widthDp = 16f)
        } else {
            null
        }
    }
    val chartSubtitle = when {
        zoomEnabled -> stringResource(R.string.statistics_chart_subtitle_zoom)
        periods.size > scrollThreshold -> stringResource(R.string.statistics_chart_subtitle_scroll)
        else -> stringResource(R.string.statistics_chart_subtitle)
    }
    val startAxisGuideline = rememberLineComponent(
        fill = fill(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)),
        thickness = 1.dp,
        shape = DashedShape(dashLengthDp = 8f, gapLengthDp = 6f),
    )
    val averageDistanceKm = remember(periods) {
        periods.takeIf { it.size > 1 }
            ?.map(PeriodStats::distanceKm)
            ?.average()
            ?.takeIf { it > 0.0 }
    }
    val averageDurationHours = remember(periods) {
        periods.takeIf { it.size > 1 }
            ?.map(PeriodStats::durationHours)
            ?.average()
            ?.takeIf { it > 0.0 }
    }
    val averageDistanceLabel = stringResource(
        R.string.statistics_average_line_label,
        averageDistanceKm?.roundToInt() ?: 0,
    )
    val averageDurationLabel = stringResource(
        R.string.statistics_average_duration_label,
        averageDurationHours?.roundToInt() ?: 0,
    )
    val averageDistanceLine = rememberLineComponent(
        fill = fill(distanceColor.copy(alpha = 0.35f)),
        thickness = 1.dp,
        shape = DashedShape(dashLengthDp = 6f, gapLengthDp = 6f),
    )
    val averageDistanceLabelComponent = rememberTextComponent(
        color = MaterialTheme.colorScheme.primary,
        textSize = 10.sp,
        padding = Insets(8f, 4f, 8f, 4f),
        background = rememberShapeComponent(
            shape = CorneredShape.rounded(allDp = 8f),
            fill = fill(MaterialTheme.colorScheme.surfaceContainerHigh),
        ),
    )
    val averageDurationLine = rememberLineComponent(
        fill = fill(durationColor.copy(alpha = 0.35f)),
        thickness = 1.dp,
        shape = DashedShape(dashLengthDp = 6f, gapLengthDp = 6f),
    )
    val averageDurationLabelComponent = rememberTextComponent(
        color = MaterialTheme.colorScheme.tertiary,
        textSize = 10.sp,
        padding = Insets(8f, 4f, 8f, 4f),
        background = rememberShapeComponent(
            shape = CorneredShape.rounded(allDp = 8f),
            fill = fill(MaterialTheme.colorScheme.surfaceContainerHigh),
        ),
    )
    val decorations = remember(
        averageDistanceKm,
        averageDurationHours,
        averageDistanceLabel,
        averageDurationLabel,
        averageDistanceLine,
        averageDistanceLabelComponent,
        averageDurationLine,
        averageDurationLabelComponent,
    ) {
        buildList {
            averageDistanceKm?.let { averageDistance ->
                add(
                    HorizontalLine(
                        y = { averageDistance },
                        line = averageDistanceLine,
                        labelComponent = averageDistanceLabelComponent,
                        label = { averageDistanceLabel },
                        horizontalLabelPosition = Position.Horizontal.End,
                        verticalLabelPosition = Position.Vertical.Top,
                        verticalAxisPosition = Axis.Position.Vertical.Start,
                    ),
                )
            }
            averageDurationHours?.let { averageDuration ->
                add(
                    HorizontalLine(
                        y = { averageDuration },
                        line = averageDurationLine,
                        labelComponent = averageDurationLabelComponent,
                        label = { averageDurationLabel },
                        horizontalLabelPosition = Position.Horizontal.Start,
                        verticalLabelPosition = Position.Vertical.Top,
                        verticalAxisPosition = Axis.Position.Vertical.End,
                    ),
                )
            }
        }
    }

    LaunchedEffect(periods, chartDistanceValues) {
        modelProducer.runTransaction {
            columnSeries { series(chartDistanceValues) }
            lineSeries { series(periods.map { it.durationHours }) }
            extras { store ->
                store.set(statisticsTourCountsKey, periods.map { it.tourCount })
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.statistics_section_overview),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = chartSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StatisticsLegendRow(
                distanceColor = distanceColor,
                durationColor = durationColor,
            )
            if (periods.isEmpty()) {
                EmptyStatisticsCard()
            } else {
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberLineCartesianLayer(
                            lineProvider = LineCartesianLayer.LineProvider.series(
                                LineCartesianLayer.rememberLine(
                                    fill = LineCartesianLayer.LineFill.single(fill(durationColor)),
                                ),
                            ),
                            verticalAxisPosition = Axis.Position.Vertical.End,
                        ),
                        rememberColumnCartesianLayer(
                            columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                                rememberLineComponent(
                                    fill = fill(distanceColor),
                                    thickness = STATISTICS_COLUMN_WIDTH_DP.dp,
                                    shape = CorneredShape.rounded(topLeftDp = 6f, topRightDp = 6f),
                                ),
                            ),
                            columnCollectionSpacing = STATISTICS_COLUMN_COLLECTION_SPACING_DP.dp,
                            dataLabel = rememberTextComponent(
                                color = MaterialTheme.colorScheme.onSurface,
                                textSize = 11.sp,
                                padding = Insets(4f, 2f, 4f, 2f),
                                background = rememberShapeComponent(
                                    shape = CorneredShape.rounded(allPercent = 50),
                                    fill = fill(MaterialTheme.colorScheme.surfaceContainer),
                                ),
                            ),
                            dataLabelValueFormatter = remember(chartDistanceValueIndices) {
                                CartesianValueFormatter { context, value, _ ->
                                    val tourCounts = context.model.extraStore.getOrNull(statisticsTourCountsKey)
                                        ?: return@CartesianValueFormatter ""
                                    chartDistanceValueIndices[value]
                                        ?.let(tourCounts::getOrNull)
                                        ?.toString()
                                        .orEmpty()
                                }
                            },
                            verticalAxisPosition = Axis.Position.Vertical.Start,
                        ),
                        startAxis = VerticalAxis.rememberStart(
                            title = stringResource(R.string.statistics_axis_distance),
                            guideline = startAxisGuideline,
                            valueFormatter = remember {
                                CartesianValueFormatter { _, value, _ -> "${value.roundToInt()} km" }
                            },
                        ),
                        endAxis = VerticalAxis.rememberEnd(
                            title = stringResource(R.string.statistics_axis_secondary),
                            guideline = null,
                            valueFormatter = remember {
                                CartesianValueFormatter { _, value, _ -> "${value.roundToInt()} h" }
                            },
                        ),
                        bottomAxis = HorizontalAxis.rememberBottom(
                            labelRotationDegrees = if (periods.size > STATISTICS_BOTTOM_AXIS_ROTATION_THRESHOLD) 45f else 0f,
                            valueFormatter = remember(periods) {
                                CartesianValueFormatter { _, value, _ ->
                                    periods.getOrNull(value.roundToInt())?.label.orEmpty()
                                }
                            },
                        ),
                        marker = marker,
                        markerVisibilityListener = markerVisibilityListener,
                        markerController = markerController,
                        fadingEdges = fadingEdges,
                        decorations = decorations,
                    ),
                    modelProducer = modelProducer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    scrollState = scrollState,
                    zoomState = zoomState,
                )
            }
        }
    }
}

@Composable
private fun rememberStatisticsMarker(
    periods: List<PeriodStats>,
    distanceColor: Color,
) : DefaultCartesianMarker {
    val indicatorStrokeColor = MaterialTheme.colorScheme.surfaceContainerHigh

    return rememberDefaultCartesianMarker(
        label = rememberTextComponent(
            color = MaterialTheme.colorScheme.onSurface,
            textSize = 12.sp,
            padding = Insets(12f, 8f, 12f, 8f),
            background = rememberShapeComponent(
                shape = CorneredShape.rounded(allDp = 12f),
                fill = fill(MaterialTheme.colorScheme.surfaceBright),
                strokeFill = fill(MaterialTheme.colorScheme.outlineVariant),
                strokeThickness = 1.dp,
            ),
        ),
        valueFormatter = remember(periods) {
            DefaultCartesianMarker.ValueFormatter { _, targets ->
                val period = targets.firstOrNull()
                    ?.x
                    ?.roundToInt()
                    ?.let(periods::getOrNull)
                    ?: return@ValueFormatter ""
                buildString {
                    append(period.label)
                    append('\n')
                    append(period.tourCount)
                    append(" • ")
                    append(period.distanceKm.toReadableDistance())
                    append(" • ")
                    append(period.durationHours.toReadableHours())
                }
            }
        },
        labelPosition = DefaultCartesianMarker.LabelPosition.Top,
        indicator = { color ->
            shapeComponent(
                fill = fill(color),
                shape = CorneredShape.Pill,
                strokeFill = fill(indicatorStrokeColor),
                strokeThickness = 2.dp,
            )
        },
        guideline = rememberLineComponent(
            fill = fill(distanceColor.copy(alpha = 0.45f)),
            thickness = 1.dp,
            shape = DashedShape(),
        ),
    )
}

@Composable
private fun StatisticsDetailCard(
    period: PeriodStats,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = period.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = period.dateRangeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatisticsMetricTile(
                    label = stringResource(R.string.statistics_label_tours),
                    value = period.tourCount.toString(),
                    modifier = Modifier.weight(1f),
                )
                StatisticsMetricTile(
                    label = stringResource(R.string.statistics_label_distance),
                    value = period.distanceKm.toReadableDistance(),
                    modifier = Modifier.weight(1f),
                )
                StatisticsMetricTile(
                    label = stringResource(R.string.statistics_label_duration),
                    value = period.durationHours.toReadableHours(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StatisticsLegendRow(
    distanceColor: Color,
    durationColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem(distanceColor, "${stringResource(R.string.statistics_legend_distance)} km")
        LegendItem(durationColor, "${stringResource(R.string.statistics_legend_duration)} h")
        TourCountLegendItem(label = stringResource(R.string.statistics_label_tours))
    }
}

@Composable
private fun TourCountLegendItem(label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Text(
                text = "n",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
        Surface(
            color = color,
            shape = MaterialTheme.shapes.extraSmall,
            modifier = Modifier.size(10.dp),
        ) {}
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyStatisticsCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.statistics_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.statistics_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatisticsMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun List<CartesianMarker.Target>.updateSelection(
    periods: List<PeriodStats>,
    onPeriodSelected: (Long) -> Unit,
) {
    val period = firstOrNull()
        ?.x
        ?.roundToInt()
        ?.let(periods::getOrNull)
        ?: return
    onPeriodSelected(period.startEpochMillis)
}

private fun List<PeriodStats>.toChartDistanceValues(): List<Double> {
    val duplicateCounts = mutableMapOf<Double, Int>()

    return map { period ->
        val duplicateIndex = duplicateCounts.getOrDefault(period.distanceKm, 0)
        duplicateCounts[period.distanceKm] = duplicateIndex + 1

        // Preserve the rendered height while giving duplicate values a unique lookup key.
        var chartValue = period.distanceKm
        repeat(duplicateIndex) {
            chartValue = Math.nextUp(chartValue)
        }
        chartValue
    }
}
