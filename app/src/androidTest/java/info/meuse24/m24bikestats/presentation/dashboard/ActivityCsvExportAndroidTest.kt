package info.meuse24.m24bikestats.presentation.dashboard

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActivityCsvExportAndroidTest {

    @Test
    fun createActivitiesCsvUri_writesSummaryCsvFile() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val uri = createActivitiesCsvUri(
            context = context,
            export = ActivitiesCsvExportUiModel(
                fileName = "bosch-activities-test.csv",
                csvContent = "id,title\n1,Tour",
                activityCount = 1,
            ),
        )

        val content = readText(context, uri)

        assertTrue(content.contains("id,title"))
        assertTrue(content.contains("1,Tour"))
    }

    @Test
    fun createActivityDetailsCsvUri_writesDetailCsvFile() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val uri = createActivityDetailsCsvUri(
            context = context,
            export = ActivityDetailsCsvExportUiModel(
                fileName = "bosch-activity-details-test.csv",
                csvContent = "activity_id,point_index\n1,0",
                activityCount = 1,
                detailPointCount = 1,
            ),
        )

        val content = readText(context, uri)

        assertTrue(content.contains("activity_id,point_index"))
        assertTrue(content.contains("1,0"))
    }

    private fun readText(context: android.content.Context, uri: Uri): String =
        context.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
}
