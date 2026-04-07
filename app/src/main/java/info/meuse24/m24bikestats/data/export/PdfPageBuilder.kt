package info.meuse24.m24bikestats.data.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import info.meuse24.m24bikestats.domain.model.PdfReportPeriod
import info.meuse24.m24bikestats.shared.map.GeoPoint
import info.meuse24.m24bikestats.shared.map.MapTileViewport
import info.meuse24.m24bikestats.shared.map.computeGeoBounds
import info.meuse24.m24bikestats.shared.map.createDiscreteMapViewport
import info.meuse24.m24bikestats.shared.map.inflateGeoBounds
import info.meuse24.m24bikestats.shared.map.latitudeToWorldY
import info.meuse24.m24bikestats.shared.map.longitudeToWorldX
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow

class PdfPageBuilder(
    private val document: PdfDocument,
    private val typography: PdfTypography,
    private val colors: PdfColorScheme,
    private val footerText: String,
) {
    private var currentPage: PdfDocument.Page? = null
    private var canvas: Canvas? = null
    private var yCursor: Float = CONTENT_TOP
    private var pageNumber: Int = 0

    fun startPage() {
        finishCurrentPage()
        pageNumber += 1
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        currentPage = document.startPage(pageInfo)
        canvas = currentPage!!.canvas
        yCursor = CONTENT_TOP
    }

    fun drawCoverTitle(
        title: String,
        subtitle: String,
    ) {
        drawMultilineText(title, typography.coverTitle, CONTENT_LEFT, yCursor, CONTENT_WIDTH)
        yCursor += 40f
        drawMultilineText(subtitle, typography.coverSubtitle, CONTENT_LEFT, yCursor, CONTENT_WIDTH)
        yCursor += 24f
        requireCanvas().drawLine(CONTENT_LEFT, yCursor, CONTENT_RIGHT, yCursor, dividerPaint())
        yCursor += 18f
    }

    fun drawSectionHeader(text: String) {
        // Keep a small minimum content area below a section heading to avoid orphan headers at page bottom.
        ensureSpace(56f)
        drawMultilineText(text, typography.sectionTitle, CONTENT_LEFT, yCursor, CONTENT_WIDTH)
        yCursor += 24f
        requireCanvas().drawLine(CONTENT_LEFT, yCursor, CONTENT_RIGHT, yCursor, dividerPaint())
        yCursor += 12f
    }

    fun drawBodyText(text: String) {
        ensureSpace(28f)
        yCursor += drawMultilineText(text, typography.body, CONTENT_LEFT, yCursor, CONTENT_WIDTH) + 8f
    }

    fun drawLabelValueRow(
        label: String,
        value: String,
    ) {
        ensureSpace(34f)
        val labelWidth = 150
        val labelHeight = drawMultilineText(label, typography.label, CONTENT_LEFT, yCursor, labelWidth)
        val valueHeight = drawMultilineText(
            value,
            typography.value,
            CONTENT_LEFT + labelWidth + 12f,
            yCursor,
            (CONTENT_WIDTH - labelWidth - 12f).toInt(),
        )
        yCursor += max(labelHeight, valueHeight) + 10f
        requireCanvas().drawLine(CONTENT_LEFT, yCursor, CONTENT_RIGHT, yCursor, faintDividerPaint())
        yCursor += 8f
    }

    fun drawMetricTiles(
        items: List<Pair<String, String>>,
        columns: Int,
    ) {
        if (items.isEmpty()) return
        val tileSpacing = 10f
        val tileWidth = (CONTENT_WIDTH - (tileSpacing * (columns - 1))) / columns
        val tileHeight = 72f

        items.chunked(columns).forEach { row ->
            ensureSpace(tileHeight + 10f)
            row.forEachIndexed { index, (label, value) ->
                val left = CONTENT_LEFT + index * (tileWidth + tileSpacing)
                drawMetricTile(left, yCursor, tileWidth, tileHeight, label, value)
            }
            yCursor += tileHeight + 10f
        }
    }

    fun drawBarLineChart(
        periods: List<PdfReportPeriod>,
        avgDistanceKm: Double,
        avgDurationHours: Double,
        distanceLegend: String,
        durationLegend: String,
    ) {
        ensureSpace(280f)
        val localCanvas = requireCanvas()
        val chartTop = yCursor + 8f
        val chartHeight = 170f
        val chartBottom = chartTop + chartHeight
        val axisBottom = chartBottom + 24f
        val leftAxisWidth = 28f
        val rightAxisWidth = 28f
        val chartLeft = CONTENT_LEFT + leftAxisWidth
        val chartRight = CONTENT_RIGHT - rightAxisWidth
        val chartWidth = chartRight - chartLeft
        val maxDistance = max(
            periods.maxOfOrNull { it.distanceKm }?.toFloat() ?: 0f,
            avgDistanceKm.toFloat(),
        ).takeIf { it > 0f }?.times(1.15f) ?: 1f
        val maxDuration = max(
            periods.maxOfOrNull { it.durationHours }?.toFloat() ?: 0f,
            avgDurationHours.toFloat(),
        ).takeIf { it > 0f }?.times(1.15f) ?: 1f

        repeat(5) { step ->
            val ratio = step / 4f
            val y = chartBottom - ratio * chartHeight
            localCanvas.drawLine(chartLeft, y, chartRight, y, faintDividerPaint())
            localCanvas.drawText(
                ((maxDistance * ratio).toInt()).toString(),
                CONTENT_LEFT,
                y + 3f,
                typography.chartLabel,
            )
            localCanvas.drawText(
                String.format("%.1f", maxDuration * ratio),
                chartRight + 6f,
                y + 3f,
                typography.chartLabel,
            )
        }

        if (periods.isNotEmpty()) {
            val slotWidth = chartWidth / periods.size
            val barWidth = slotWidth * 0.56f
            val linePath = Path()
            periods.forEachIndexed { index, period ->
                val centerX = chartLeft + (slotWidth * index) + (slotWidth / 2f)
                val barHeight = (period.distanceKm.toFloat() / maxDistance) * chartHeight
                localCanvas.drawRoundRect(
                    RectF(centerX - barWidth / 2f, chartBottom - barHeight, centerX + barWidth / 2f, chartBottom),
                    8f,
                    8f,
                    fillPaint(colors.primary),
                )
                localCanvas.drawText(period.tourCount.toString(), centerX - 4f, chartBottom - barHeight - 6f, typography.chartLabel)

                val lineY = chartBottom - ((period.durationHours.toFloat() / maxDuration) * chartHeight)
                if (index == 0) {
                    linePath.moveTo(centerX, lineY)
                } else {
                    linePath.lineTo(centerX, lineY)
                }
                localCanvas.drawCircle(centerX, lineY, 3.5f, fillPaint(colors.secondary))
                localCanvas.save()
                localCanvas.rotate(30f, centerX, axisBottom + 8f)
                localCanvas.drawText(period.label, centerX - 12f, axisBottom + 8f, typography.chartLabel)
                localCanvas.restore()
            }
            localCanvas.drawPath(linePath, strokePaint(colors.secondary, 2f))
        }

        val avgDistanceY = chartBottom - ((avgDistanceKm.toFloat() / maxDistance) * chartHeight)
        localCanvas.drawLine(chartLeft, avgDistanceY, chartRight, avgDistanceY, dashedPaint(colors.primary))
        val avgDurationY = chartBottom - ((avgDurationHours.toFloat() / maxDuration) * chartHeight)
        localCanvas.drawLine(chartLeft, avgDurationY, chartRight, avgDurationY, dashedPaint(colors.secondary))

        val legendTop = axisBottom + 28f
        localCanvas.drawRect(CONTENT_LEFT, legendTop, CONTENT_LEFT + 10f, legendTop + 10f, fillPaint(colors.primary))
        localCanvas.drawText(distanceLegend, CONTENT_LEFT + 16f, legendTop + 9f, typography.bodyMuted)
        localCanvas.drawRect(CONTENT_LEFT + 150f, legendTop, CONTENT_LEFT + 160f, legendTop + 10f, fillPaint(colors.secondary))
        localCanvas.drawText(durationLegend, CONTENT_LEFT + 166f, legendTop + 9f, typography.bodyMuted)

        yCursor = legendTop + 20f
    }

    fun drawHorizontalBarChart(
        rows: List<Pair<String, Int>>,
        highlightLabel: String? = null,
    ) {
        if (rows.isEmpty()) return
        val maxValue = rows.maxOf { it.second }.coerceAtLeast(1)
        rows.forEach { (label, value) ->
            ensureSpace(28f)
            drawMultilineText(label, typography.body, CONTENT_LEFT, yCursor, 100)
            val barLeft = CONTENT_LEFT + 110f
            val barTop = yCursor + 2f
            val barWidth = ((CONTENT_RIGHT - barLeft - 36f) * (value.toFloat() / maxValue.toFloat()))
            requireCanvas().drawRoundRect(
                RectF(barLeft, barTop, barLeft + barWidth, barTop + 12f),
                6f,
                6f,
                fillPaint(if (label == highlightLabel) colors.primary else colors.accent),
            )
            requireCanvas().drawText(value.toString(), CONTENT_RIGHT - 24f, yCursor + 12f, typography.body)
            yCursor += 22f
        }
        yCursor += 4f
    }

    fun drawFrequencyTable(
        rows: List<Pair<String, String>>,
        highlightedRowIndex: Int?,
    ) {
        rows.forEachIndexed { index, (label, value) ->
            ensureSpace(28f)
            if (highlightedRowIndex == index) {
                requireCanvas().drawRoundRect(
                    RectF(CONTENT_LEFT, yCursor - 2f, CONTENT_RIGHT, yCursor + 18f),
                    8f,
                    8f,
                    fillPaint(colors.highlight),
                )
            }
            drawMultilineText(label, typography.body, CONTENT_LEFT + 8f, yCursor, 240)
            drawMultilineText(value, typography.heading, CONTENT_RIGHT - 100f, yCursor, 92)
            yCursor += 22f
        }
        yCursor += 4f
    }

    fun drawProgressBar(
        label: String,
        ratio: Double,
    ) {
        ensureSpace(42f)
        drawMultilineText(label, typography.body, CONTENT_LEFT, yCursor, CONTENT_WIDTH)
        yCursor += 18f
        requireCanvas().drawRoundRect(
            RectF(CONTENT_LEFT, yCursor, CONTENT_RIGHT, yCursor + 10f),
            6f,
            6f,
            fillPaint(colors.surfaceMuted),
        )
        requireCanvas().drawRoundRect(
            RectF(CONTENT_LEFT, yCursor, CONTENT_LEFT + (CONTENT_WIDTH * ratio.coerceIn(0.0, 1.0)).toFloat(), yCursor + 10f),
            6f,
            6f,
            fillPaint(colors.primary),
        )
        yCursor += 20f
    }

    fun drawRoutePointMap(
        points: List<Pair<Double, Double>>,
        legend: String,
        tileProvider: ((zoom: Int, x: Int, y: Int) -> Bitmap?)? = null,
    ) {
        ensureSpace(320f)
        val localCanvas = requireCanvas()
        val mapTop = yCursor + 8f
        val mapHeight = 238f
        val mapBottom = mapTop + mapHeight
        val mapRect = RectF(CONTENT_LEFT, mapTop, CONTENT_RIGHT, mapBottom)
        val viewport = points.toMapViewport(mapRect.width().toDouble(), mapRect.height().toDouble())

        localCanvas.drawRoundRect(mapRect, 12f, 12f, fillPaint(colors.surfaceMuted))
        localCanvas.drawRoundRect(mapRect, 12f, 12f, strokePaint(colors.border, 1f))
        drawTileBackground(localCanvas, mapRect, viewport, tileProvider)
        drawOverlayGrid(localCanvas, mapRect)
        drawRoutePoints(localCanvas, mapRect, viewport, points)

        drawMultilineText(legend, typography.bodyMuted, CONTENT_LEFT, mapBottom + 12f, CONTENT_WIDTH)
        yCursor = mapBottom + 36f
    }

    fun space(height: Float) {
        ensureSpace(height)
        yCursor += height
    }

    fun finish() {
        finishCurrentPage()
    }

    private fun ensureSpace(height: Float) {
        if (currentPage == null) {
            startPage()
        } else if (yCursor + height > PAGE_HEIGHT - BOTTOM_MARGIN - 24f) {
            startPage()
        }
    }

    private fun finishCurrentPage() {
        val page = currentPage ?: return
        val localCanvas = canvas ?: return
        localCanvas.drawLine(CONTENT_LEFT, PAGE_HEIGHT - 38f, CONTENT_RIGHT, PAGE_HEIGHT - 38f, faintDividerPaint())
        localCanvas.drawText(footerText, CONTENT_LEFT, PAGE_HEIGHT - 24f, typography.footer)
        localCanvas.drawText(pageNumber.toString(), CONTENT_RIGHT - 12f, PAGE_HEIGHT - 24f, typography.footer)
        document.finishPage(page)
        currentPage = null
        canvas = null
    }

    private fun drawMetricTile(
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        label: String,
        value: String,
    ) {
        val localCanvas = requireCanvas()
        localCanvas.drawRoundRect(RectF(left, top, left + width, top + height), 12f, 12f, fillPaint(colors.surfaceMuted))
        localCanvas.drawRoundRect(RectF(left, top, left + width, top + height), 12f, 12f, strokePaint(colors.border, 1f))
        drawMultilineText(label, typography.tileLabel, left + 10f, top + 10f, (width - 20f).toInt())
        drawMultilineText(value, typography.tileValue, left + 10f, top + 30f, (width - 20f).toInt())
    }

    private fun drawMultilineText(
        text: String,
        paint: TextPaint,
        x: Float,
        y: Float,
        width: Int,
    ): Float {
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .build()
        requireCanvas().save()
        requireCanvas().translate(x, y)
        layout.draw(requireCanvas())
        requireCanvas().restore()
        return layout.height.toFloat()
    }

    private fun drawTileBackground(
        canvas: Canvas,
        mapRect: RectF,
        viewport: MapTileViewport,
        tileProvider: ((zoom: Int, x: Int, y: Int) -> Bitmap?)?,
    ) {
        if (tileProvider == null) return
        val tilesPerAxis = 2.0.pow(viewport.zoom).toInt().coerceAtLeast(1)
        val tileSize = PDF_TILE_SIZE
        val startTileX = floor(viewport.topLeftWorldX / tileSize).toInt()
        val endTileX = floor((viewport.topLeftWorldX + mapRect.width()) / tileSize).toInt()
        val startTileY = floor(viewport.topLeftWorldY / tileSize).toInt()
        val endTileY = floor((viewport.topLeftWorldY + mapRect.height()) / tileSize).toInt()

        canvas.save()
        canvas.clipRect(mapRect)
        for (tileY in startTileY..endTileY) {
            if (tileY !in 0 until tilesPerAxis) continue
            for (tileX in startTileX..endTileX) {
                val wrappedX = ((tileX % tilesPerAxis) + tilesPerAxis) % tilesPerAxis
                val tile = tileProvider(viewport.zoom, wrappedX, tileY) ?: continue
                val left = mapRect.left + ((tileX * tileSize) - viewport.topLeftWorldX).toFloat()
                val top = mapRect.top + ((tileY * tileSize) - viewport.topLeftWorldY).toFloat()
                val dest = RectF(left, top, left + tileSize.toFloat(), top + tileSize.toFloat())
                canvas.drawBitmap(tile, null, dest, null)
            }
        }
        canvas.restore()
    }

    private fun drawOverlayGrid(
        canvas: Canvas,
        mapRect: RectF,
    ) {
        canvas.save()
        canvas.clipRect(mapRect)
        repeat(7) { step ->
            val x = mapRect.left + (mapRect.width() * step / 6f)
            canvas.drawLine(x, mapRect.top, x, mapRect.bottom, faintDividerPaint())
        }
        repeat(5) { step ->
            val y = mapRect.top + (mapRect.height() * step / 4f)
            canvas.drawLine(mapRect.left, y, mapRect.right, y, faintDividerPaint())
        }
        canvas.restore()
    }

    private fun drawRoutePoints(
        canvas: Canvas,
        mapRect: RectF,
        viewport: MapTileViewport,
        points: List<Pair<Double, Double>>,
    ) {
        canvas.save()
        canvas.clipRect(mapRect)
        points.forEach { (latitude, longitude) ->
            val x = mapRect.left + (longitudeToWorldX(longitude, PDF_TILE_SIZE, viewport.zoom.toDouble()) - viewport.topLeftWorldX).toFloat()
            val y = mapRect.top + (latitudeToWorldY(latitude, PDF_TILE_SIZE, viewport.zoom.toDouble()) - viewport.topLeftWorldY).toFloat()
            canvas.drawCircle(x, y, 4.6f, fillPaint(colors.primary))
            canvas.drawCircle(x, y, 5.8f, strokePaint(colors.surface, 1.3f))
        }
        canvas.restore()
    }

    private fun List<Pair<Double, Double>>.toMapViewport(
        mapWidthPx: Double,
        mapHeightPx: Double,
    ): MapTileViewport = createDiscreteMapViewport(
        bounds = inflateGeoBounds(
            bounds = computeGeoBounds(map { (latitude, longitude) -> GeoPoint(latitude, longitude) }),
            spanScale = 1.25,
            minLatitudeSpan = 0.02,
        ),
        mapWidthPx = mapWidthPx,
        mapHeightPx = mapHeightPx,
        tileSize = PDF_TILE_SIZE,
        minCoordinateDelta = 0.0002,
        zoomAdjustment = 0.15,
        minZoom = 2.5,
        maxZoom = 15.0,
    )

    private fun requireCanvas(): Canvas =
        canvas ?: error("No active PDF page. Call startPage() before drawing.")

    private fun fillPaint(color: Int): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = color
        }

    private fun strokePaint(color: Int, strokeWidth: Float): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.color = color
            this.strokeWidth = strokeWidth
        }

    private fun dividerPaint(): Paint = strokePaint(colors.divider, 1.3f)

    private fun faintDividerPaint(): Paint = strokePaint(colors.divider, 0.8f)

    private fun dashedPaint(color: Int): Paint =
        strokePaint(color, 1.3f).apply {
            pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
        }

    companion object {
        // PDF map rendering uses direct raster tiles (256 px) from OSM-compatible providers.
        private const val PDF_TILE_SIZE = 256.0
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val CONTENT_LEFT = 40f
        const val CONTENT_RIGHT = 555f
        const val CONTENT_TOP = 60f
        const val CONTENT_WIDTH = (CONTENT_RIGHT - CONTENT_LEFT).toInt()
        const val BOTTOM_MARGIN = 40f
    }

}
