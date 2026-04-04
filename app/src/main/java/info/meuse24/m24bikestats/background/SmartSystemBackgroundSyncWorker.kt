package info.meuse24.m24bikestats.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import info.meuse24.m24bikestats.domain.model.CloudSyncDetailMode
import info.meuse24.m24bikestats.domain.usecase.SyncSmartSystemCloudUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SmartSystemBackgroundSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {

    private val syncSmartSystemCloudUseCase: SyncSmartSystemCloudUseCase by inject()

    override suspend fun doWork(): Result {
        val detailMode = CloudSyncDetailMode.fromStoredValue(
            inputData.getString(BackgroundSyncScheduler.INPUT_DETAIL_MODE),
        ) ?: CloudSyncDetailMode.MISSING_ONLY

        return syncSmartSystemCloudUseCase(detailMode = detailMode) { }.fold(
            onSuccess = { Result.success() },
            onFailure = {
                if (runAttemptCount < MAX_RETRY_COUNT) Result.retry() else Result.failure()
            },
        )
    }

    private companion object {
        private const val MAX_RETRY_COUNT = 3
    }
}
