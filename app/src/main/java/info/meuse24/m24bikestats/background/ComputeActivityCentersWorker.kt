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
        val detailIds = activityDetailDao.getAllMetadata()
            .map { it.activityId }
            .distinct()
            .toSet()
        if (detailIds.isEmpty()) return Result.success()

        val preferences = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val fullRecomputeDone = preferences.getBoolean(KEY_FULL_RECOMPUTE_DONE, false)

        if (!fullRecomputeDone) {
            recomputeCenters(detailIds)
            preferences.edit().putBoolean(KEY_FULL_RECOMPUTE_DONE, true).apply()
            return Result.success()
        }

        val idsWithoutCenter = activityDao.getIdsWithoutCenter()
            .asSequence()
            .filter { it in detailIds }
            .distinct()
            .toList()
        val staleCenterIds = activityDao.getIdsWithCenterButWithoutGpsPointsInDetails()
        if (idsWithoutCenter.isEmpty() && staleCenterIds.isEmpty()) return Result.success()

        recomputeCenters(idsWithoutCenter.toSet())
        for (id in staleCenterIds) {
            activityDao.clearCenter(id)
        }
        return Result.success()
    }

    private suspend fun recomputeCenters(ids: Set<String>) {
        for (id in ids) {
            val points = activityDetailDao.getGpsPointsForActivity(id).map { it.latitude to it.longitude }
            val center = ActivityCenterCalculator.calculate(points)
            if (center == null) {
                activityDao.clearCenter(id)
                continue
            }
            activityDao.updateCenter(id, center.first, center.second)
        }
    }

    companion object {
        const val WORK_NAME = "compute_activity_representative_points_once"
        private const val PREFS_NAME = "representative_point_worker_state"
        private const val KEY_FULL_RECOMPUTE_DONE = "full_recompute_done"
    }
}
