package info.meuse24.m24bikestats.presentation.dashboard

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import info.meuse24.m24bikestats.domain.model.CsvExportFormat
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackSharingAndroidTest {

    @Test
    fun createTrackCsvUri_writesCsvFile() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val uri = createTrackCsvUri(context, testActivityDetail(), CsvExportFormat.STANDARD_INTERNATIONAL)

        val content = readText(context, uri)

        assertTrue(content.contains("point_index,latitude,longitude"))
        assertTrue(content.contains("47.100000,9.100000"))
    }

    @Test
    fun createTrackGpxUri_writesGpxFile() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val uri = createTrackGpxUri(context, testActivityDetail())

        val content = readText(context, uri)

        assertTrue(content.contains("<gpx version=\"1.1\""))
        assertTrue(content.contains("<trkpt lat=\"47.1\" lon=\"9.1\">"))
    }

    private fun readText(context: android.content.Context, uri: Uri): String =
        context.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }

    private fun testActivityDetail() = ActivityDetailUiModel(
        id = "a1",
        title = "Morgenrunde",
        subtitle = null,
        overview = ActivityCardUiModel(
            id = "a1",
            title = "Morgenrunde",
            startedAt = "2026-04-03T10:00:00Z",
            startedAtEpochMillis = 1_743_678_000_000,
            distanceMeters = 12_300,
            durationSeconds = 1800,
            dateLabel = "03.04.2026 10:00",
            distanceLabel = "12.3 km",
            durationLabel = "30 min",
            speedLabel = "24.5 km/h",
            powerLabel = null,
            elevationLabel = null,
            caloriesLabel = null,
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
