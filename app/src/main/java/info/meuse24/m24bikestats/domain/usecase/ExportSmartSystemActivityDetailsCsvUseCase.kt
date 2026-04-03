package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.model.BoschActivityDetail
import info.meuse24.m24bikestats.domain.model.BoschActivityDetailsCsvExport
import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ExportSmartSystemActivityDetailsCsvUseCase(
    private val repository: BoschSmartSystemRepository,
    private val authRepository: AuthRepository,
    private val detailCacheTtlMillis: Long = DEFAULT_DETAIL_CACHE_TTL_MILLIS,
) {
    suspend operator fun invoke(
        activityIds: List<String>,
        onProgress: (processedCount: Int, totalCount: Int) -> Unit = { _, _ -> },
    ): Result<BoschActivityDetailsCsvExport> {
        val normalizedIds = activityIds.distinct()
        if (normalizedIds.isEmpty()) {
            return Result.failure(IllegalArgumentException("Keine Aktivitäten für den Detail-Export ausgewählt"))
        }

        val token = authRepository.getValidAccessToken()
            .getOrElse { return Result.failure(it) }

        val detailRows = mutableListOf<String>()
        var exportedPointCount = 0

        normalizedIds.forEachIndexed { index, activityId ->
            val activity = repository.getCachedActivity(activityId)
                ?: return Result.failure(IllegalStateException("Aktivität $activityId ist nicht im Cache verfügbar"))

            val detail = loadActivityDetail(token, activityId)
                .getOrElse { return Result.failure(it) }

            detailRows += buildRows(activity, detail)
            exportedPointCount += detail.points.size
            onProgress(index + 1, normalizedIds.size)
        }

        val timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm", Locale.US))

        return Result.success(
            BoschActivityDetailsCsvExport(
                fileName = "bosch-activity-details-$timestamp.csv",
                csvContent = buildString {
                    appendLine(CSV_HEADER)
                    detailRows.forEach(::appendLine)
                },
                activityCount = normalizedIds.size,
                detailPointCount = exportedPointCount,
            )
        )
    }

    private suspend fun loadActivityDetail(
        token: String,
        activityId: String,
    ): Result<BoschActivityDetail> {
        val cachedDetail = repository.getCachedActivityDetail(activityId)
        val isFresh = repository.isActivityDetailCacheFresh(activityId, detailCacheTtlMillis)
        if (cachedDetail != null && isFresh) {
            return Result.success(cachedDetail)
        }

        val remoteDetail = repository.getActivityDetail(token, activityId)
        return remoteDetail.fold(
            onSuccess = { Result.success(it) },
            onFailure = { error ->
                cachedDetail?.let { Result.success(it) } ?: Result.failure(error)
            }
        )
    }

    private fun buildRows(
        activity: BoschActivity,
        detail: BoschActivityDetail,
    ): List<String> = detail.points.mapIndexed { index, point ->
        listOf(
            activity.id,
            activity.title,
            activity.startTime,
            activity.endTime.orEmpty(),
            activity.bikeId.orEmpty(),
            index.toString(),
            point.latitude?.toCsvNumber().orEmpty(),
            point.longitude?.toCsvNumber().orEmpty(),
            point.distanceMeters?.toCsvNumber().orEmpty(),
            point.altitudeMeters?.toCsvNumber().orEmpty(),
            point.speedKmh?.toCsvNumber().orEmpty(),
            point.cadenceRpm?.toCsvNumber().orEmpty(),
            point.riderPowerWatts?.toCsvNumber().orEmpty(),
        ).joinToString(separator = ",") { it.escapeCsv() }
    }

    private fun Double.toCsvNumber(): String =
        String.format(Locale.US, "%.6f", this)

    private fun String.escapeCsv(): String {
        val escaped = replace("\"", "\"\"")
        return "\"$escaped\""
    }

    companion object {
        private const val DEFAULT_DETAIL_CACHE_TTL_MILLIS = 30 * 60 * 1000L
        private const val CSV_HEADER =
            "\"activity_id\",\"activity_title\",\"start_time\",\"end_time\",\"bike_id\",\"point_index\",\"latitude\",\"longitude\",\"distance_meters\",\"altitude_meters\",\"speed_kmh\",\"cadence_rpm\",\"rider_power_watts\""
    }
}
