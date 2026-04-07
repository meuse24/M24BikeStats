package info.meuse24.m24bikestats.shared.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MapGeometryTest {

    @Test
    fun `compute geo bounds rejects empty input`() {
        try {
            computeGeoBounds(emptyList())
            fail("Expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            assertEquals("computeGeoBounds requires at least one point.", expected.message)
        }
    }

    @Test
    fun `compute geo bounds supports average point center`() {
        val bounds = computeGeoBounds(
            points = listOf(
                GeoPoint(latitude = 48.0, longitude = 16.0),
                GeoPoint(latitude = 49.0, longitude = 18.0),
                GeoPoint(latitude = 50.0, longitude = 20.0),
            ),
            centerStrategy = BoundsCenterStrategy.AVERAGE_POINTS,
        )

        assertEquals(48.0, bounds.minLatitude, 0.0)
        assertEquals(50.0, bounds.maxLatitude, 0.0)
        assertEquals(49.0, bounds.centerLatitude, 0.0)
        assertEquals(18.0, bounds.centerLongitude, 0.0)
    }

    @Test
    fun `compute geo bounds keeps single point as center`() {
        val bounds = computeGeoBounds(
            listOf(GeoPoint(latitude = 48.2082, longitude = 16.3738)),
        )

        assertEquals(48.2082, bounds.minLatitude, 0.0)
        assertEquals(48.2082, bounds.maxLatitude, 0.0)
        assertEquals(16.3738, bounds.minLongitude, 0.0)
        assertEquals(16.3738, bounds.maxLongitude, 0.0)
        assertEquals(48.2082, bounds.centerLatitude, 0.0)
        assertEquals(16.3738, bounds.centerLongitude, 0.0)
    }

    @Test
    fun `inflate geo bounds enforces minimum span`() {
        val inflated = inflateGeoBounds(
            bounds = computeGeoBounds(listOf(GeoPoint(latitude = 48.2, longitude = 16.3))),
            spanScale = 1.25,
            minLatitudeSpan = 0.02,
        )

        assertEquals(0.025, inflated.maxLatitude - inflated.minLatitude, 0.0000001)
        assertEquals(0.025, inflated.maxLongitude - inflated.minLongitude, 0.0000001)
    }

    @Test
    fun `inverse mercator reverses normalized mercator value`() {
        val latitude = 47.8765
        val normalized = mercatorYNormalized(latitude)

        assertEquals(latitude, inverseMercatorYNormalized(normalized), 0.000001)
    }

    @Test
    fun `estimate zoom to fit shrinks when padding grows`() {
        val bounds = computeGeoBounds(
            points = listOf(
                GeoPoint(latitude = 48.2, longitude = 16.3),
                GeoPoint(latitude = 48.25, longitude = 16.45),
            ),
        )

        val noPaddingZoom = estimateZoomToFit(
            bounds = bounds,
            viewportWidthPx = 1000.0,
            viewportHeightPx = 800.0,
            tileSize = 512.0,
            padding = MapViewportPadding(0.0, 0.0, 0.0),
            minCoordinateDelta = 0.0003,
            zoomAdjustment = 1.05,
            minZoom = 2.5,
            maxZoom = 14.0,
        )
        val paddedZoom = estimateZoomToFit(
            bounds = bounds,
            viewportWidthPx = 1000.0,
            viewportHeightPx = 800.0,
            tileSize = 512.0,
            padding = MapViewportPadding(100.0, 80.0, 80.0),
            minCoordinateDelta = 0.0003,
            zoomAdjustment = 1.05,
            minZoom = 2.5,
            maxZoom = 14.0,
        )

        assertTrue(noPaddingZoom > paddedZoom)
    }

    @Test
    fun `fit zoom to points clamps down to fitting zoom`() {
        val points = listOf(
            GeoPoint(latitude = 48.2, longitude = 16.3),
            GeoPoint(latitude = 48.25, longitude = 16.45),
        )

        val fittedZoom = fitZoomToPoints(
            points = points,
            centerLatitude = 48.225,
            centerLongitude = 16.375,
            initialZoom = 15.0,
            minZoom = 2.5,
            maxZoom = 15.0,
            viewportWidthPx = 1000.0,
            viewportHeightPx = 800.0,
            marginFraction = 0.1,
            tileSize = 512.0,
        )

        assertTrue(fittedZoom < 15.0)
        assertTrue(
            pointsFitInViewport(
                points = points,
                centerLatitude = 48.225,
                centerLongitude = 16.375,
                zoom = fittedZoom,
                viewportWidthPx = 1000.0,
                viewportHeightPx = 800.0,
                marginFraction = 0.1,
                tileSize = 512.0,
            ),
        )
    }

    @Test
    fun `points fit in viewport reacts to zoom level`() {
        val points = listOf(
            GeoPoint(latitude = 48.2, longitude = 16.3),
            GeoPoint(latitude = 48.25, longitude = 16.45),
        )

        assertTrue(
            pointsFitInViewport(
                points = points,
                centerLatitude = 48.225,
                centerLongitude = 16.375,
                zoom = 10.0,
                viewportWidthPx = 1000.0,
                viewportHeightPx = 800.0,
                marginFraction = 0.1,
                tileSize = 512.0,
            ),
        )
        assertFalse(
            pointsFitInViewport(
                points = points,
                centerLatitude = 48.225,
                centerLongitude = 16.375,
                zoom = 15.0,
                viewportWidthPx = 1000.0,
                viewportHeightPx = 800.0,
                marginFraction = 0.1,
                tileSize = 512.0,
            ),
        )
    }
}
