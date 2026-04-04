package info.meuse24.m24bikestats.background

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import info.meuse24.m24bikestats.domain.model.BackgroundSyncMode
import info.meuse24.m24bikestats.domain.model.CloudSyncDetailMode
import java.util.concurrent.TimeUnit

class BackgroundSyncScheduler(
    private val context: Context,
) {
    private val workManager: WorkManager by lazy { WorkManager.getInstance(context) }

    fun update(
        mode: BackgroundSyncMode,
        detailMode: CloudSyncDetailMode,
    ) {
        if (mode == BackgroundSyncMode.DISABLED) {
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            return
        }

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            buildWorkRequest(mode, detailMode),
        )
    }

    internal fun buildWorkRequest(
        mode: BackgroundSyncMode,
        detailMode: CloudSyncDetailMode,
    ): PeriodicWorkRequest =
        PeriodicWorkRequestBuilder<SmartSystemBackgroundSyncWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraintsFor(mode))
            .setInputData(
                workDataOf(
                    INPUT_DETAIL_MODE to detailMode.name,
                ),
            )
            .addTag(WORK_TAG)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()

    internal fun constraintsFor(mode: BackgroundSyncMode): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(
                when (mode) {
                    BackgroundSyncMode.DAILY_UNMETERED -> NetworkType.UNMETERED
                    BackgroundSyncMode.DAILY_CONNECTED -> NetworkType.CONNECTED
                    BackgroundSyncMode.DISABLED -> NetworkType.NOT_REQUIRED
                },
            )
            .build()

    companion object {
        internal const val UNIQUE_WORK_NAME = "smart-system-background-sync"
        internal const val WORK_TAG = "smart-system-sync"
        internal const val INPUT_DETAIL_MODE = "cloud_sync_detail_mode"
    }
}
