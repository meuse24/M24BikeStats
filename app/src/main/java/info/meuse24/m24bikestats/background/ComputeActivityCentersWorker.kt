package info.meuse24.m24bikestats.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import info.meuse24.m24bikestats.data.local.dao.ActivityDao
import info.meuse24.m24bikestats.data.local.dao.ActivityDetailDao
import info.meuse24.m24bikestats.data.local.mapper.ActivityCenterCalculator
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ComputeActivityCentersWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val activityDao: ActivityDao by inject()
    private val activityDetailDao: ActivityDetailDao by inject()

    override suspend fun doWork(): Result {
        val ids = activityDetailDao.getAllMetadata()
            .map { it.activityId }
            .distinct()
        for (id in ids) {
            val points = activityDetailDao.getGpsPointsForActivity(id)
                .map { it.latitude to it.longitude }
            val center = ActivityCenterCalculator.calculate(points)
            if (center != null) {
                activityDao.updateCenter(id, center.first, center.second)
            } else {
                activityDao.clearCenter(id)
            }
        }
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "compute_activity_representative_points_v2_once"
    }
}
