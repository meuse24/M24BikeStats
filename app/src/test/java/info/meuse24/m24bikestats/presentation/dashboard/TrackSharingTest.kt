package info.meuse24.m24bikestats.presentation.dashboard

import info.meuse24.m24bikestats.domain.model.CsvExportFormat
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class TrackSharingTest {

    @Test
    fun `buildTrackCsv contains point and metric columns`() {
        val activity = testActivityDetail()

        val csv = buildTrackCsv(
            activity = activity,
            format = CsvExportFormat.STANDARD_INTERNATIONAL,
            locale = Locale.ENGLISH,
        )

        assertTrue(csv.contains("\"point_index\",\"latitude\",\"longitude\""))
        assertTrue(csv.contains("\"47.100000\",\"9.100000\""))
        assertTrue(csv.contains("\"24.500000\""))
        assertTrue(csv.contains("\"210.000000\""))
    }

    @Test
    fun `buildTrackCsv uses german excel preset`() {
        val activity = testActivityDetail()

        val csv = buildTrackCsv(
            activity = activity,
            format = CsvExportFormat.EXCEL_DE,
            locale = Locale.GERMAN,
        )

        assertTrue(csv.contains("\"point_index\";\"latitude\";\"longitude\""))
        assertTrue(csv.contains("\"47,100000\";\"9,100000\";\"100,000000\""))
    }

    @Test
    fun `buildTrackGpx contains track points with time power and cadence`() {
        val activity = testActivityDetail()

        val gpx = buildTrackGpx(activity)

        assertTrue("GPX should contain correct trkpt, but was:\n$gpx", gpx.contains("<trkpt lat=\"47.1\" lon=\"9.1\">"))
        assertTrue("GPX should contain elevation, but was:\n$gpx", gpx.contains("<ele>500.0</ele>"))
        assertTrue("GPX should contain time, but was:\n$gpx", gpx.contains("<time>2026-04-04T12:30:00Z</time>"))
        assertTrue("GPX should contain power, but was:\n$gpx", gpx.contains("<gpxtpx:power>210</gpxtpx:power>"))
        assertTrue("GPX should contain cadence, but was:\n$gpx", gpx.contains("<gpxtpx:cad>81</gpxtpx:cad>"))
        assertTrue("GPX should contain type, but was:\n$gpx", gpx.contains("<type>Cycling</type>"))
    }

    private fun testActivityDetail() = ActivityDetailUiModel(
        id = "a1",
        title = "Morgenrunde",
        subtitle = null,
        overview = ActivityCardUiModel(
            id = "a1",
            title = "Morgenrunde",
            startedAt = "2026-04-04 10:00",
            startedAtEpochMillis = 1775304000000L, // 2026-04-04 12:00:00 UTC
            distanceMeters = 12300,
            durationSeconds = 1800,
            dateLabel = "04.04.2026 10:00",
            distanceLabel = "12.3 km",
            durationLabel = "30 min",
            speedLabel = "avg 24.5 km/h\nmax 30.0 km/h",
            powerLabel = "avg 210 W\nmax 250 W",
            elevationLabel = "+100 m / -100 m",
            caloriesLabel = "173 kcal",
        ),
        summary = listOf("Distanz" to "12.3 km"),
        sections = emptyList(),
        trackPoints = listOf(
            ActivityTrackPointUiModel(
                latitude = 47.1,
                longitude = 9.1,
                altitudeMeters = 500.0,
                distanceMeters = 100.0,
            )
        ),
        profilePoints = listOf(
            ActivityProfilePointUiModel(
                distanceMeters = 100.0,
                altitudeMeters = 500.0,
                speedKmh = 24.5,
                cadenceRpm = 81.0,
                riderPowerWatts = 210.0,
            )
        ),
    )
}
