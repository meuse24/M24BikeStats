package info.meuse24.m24bikestats

import android.app.Application
import info.meuse24.m24bikestats.background.BackgroundSyncSettingsObserver
import info.meuse24.m24bikestats.background.ComputeActivityCentersWorker
import info.meuse24.m24bikestats.di.appModule
import androidx.work.WorkManager
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Constraints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.context.GlobalContext
import org.koin.core.logger.Level

class M24BikeStatsApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@M24BikeStatsApp)
            modules(appModule)
        }
        GlobalContext.get().get<BackgroundSyncSettingsObserver>().start(applicationScope)
        WorkManager.getInstance(this).enqueueUniqueWork(
            ComputeActivityCentersWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<ComputeActivityCentersWorker>()
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
        )
    }
}
