package info.meuse24.m24bikestats

import android.app.Application
import info.meuse24.m24bikestats.background.ComputeActivityCentersWorker
import info.meuse24.m24bikestats.di.appModule
import androidx.work.WorkManager
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class M24BikeStatsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@M24BikeStatsApp)
            modules(appModule)
        }
        WorkManager.getInstance(this).enqueueUniqueWork(
            ComputeActivityCentersWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<ComputeActivityCentersWorker>().build()
        )
    }
}
