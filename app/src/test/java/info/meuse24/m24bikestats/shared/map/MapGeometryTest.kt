package info.meuse24.m24bikestats.shared.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapGeometryTest {

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
