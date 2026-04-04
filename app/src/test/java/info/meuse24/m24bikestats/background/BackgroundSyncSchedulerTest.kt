package info.meuse24.m24bikestats.background

import android.content.ContextWrapper
import androidx.work.NetworkType
import info.meuse24.m24bikestats.domain.model.BackgroundSyncMode
import info.meuse24.m24bikestats.domain.model.CloudSyncDetailMode
import org.junit.Assert.assertEquals
import org.junit.Test

class BackgroundSyncSchedulerTest {

    private val scheduler = BackgroundSyncScheduler(ContextWrapper(null))

    @Test
    fun `daily unmetered uses unmetered constraint`() {
        val constraints = scheduler.constraintsFor(BackgroundSyncMode.DAILY_UNMETERED)

        assertEquals(NetworkType.UNMETERED, constraints.requiredNetworkType)
    }

    @Test
    fun `work request stores selected detail mode`() {
        val request = scheduler.buildWorkRequest(
            mode = BackgroundSyncMode.DAILY_CONNECTED,
            detailMode = CloudSyncDetailMode.MISSING_OR_STALE,
        )

        assertEquals(
            CloudSyncDetailMode.MISSING_OR_STALE.name,
            request.workSpec.input.getString(BackgroundSyncScheduler.INPUT_DETAIL_MODE),
        )
    }
}
