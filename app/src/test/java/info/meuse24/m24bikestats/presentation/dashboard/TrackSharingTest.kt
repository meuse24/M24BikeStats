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
    fun `buildTrackGpx contains track points`() {
        val activity = testActivityDetail()

        val gpx = buildTrackGpx(activity)

        assertTrue(gpx.contains("<trkpt lat=\"47.1\" lon=\"9.1\">"))
        assertTrue(gpx.contains("<ele>500.0</ele>"))
    }

    private fun testActivityDetail() = ActivityDetailUiModel(
        id = "a1",
        title = "Morgenrunde",
        subtitle = null,
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
