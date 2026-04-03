package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.model.BoschActivityDetail
import info.meuse24.m24bikestats.domain.model.BoschActivityDetailsCsvExport
import info.meuse24.m24bikestats.domain.model.CsvDialect
import info.meuse24.m24bikestats.domain.repository.AppSettingsRepository
import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

class ExportSmartSystemActivityDetailsCsvUseCase(
    private val repository: BoschSmartSystemRepository,
    @Suppress("unused")
    private val authRepository: AuthRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val localeProvider: () -> Locale = Locale::getDefault,
) {
    suspend operator fun invoke(
        activityIds: List<String>,
        onProgress: (processedCount: Int, totalCount: Int) -> Unit = { _, _ -> },
    ): Result<BoschActivityDetailsCsvExport> {
        val normalizedIds = activityIds.distinct()
        if (normalizedIds.isEmpty()) {
            return Result.failure(IllegalArgumentException("Keine Aktivitäten für den Detail-Export ausgewählt"))
        }

        return runCatching {
            val detailRows = mutableListOf<String>()
            var exportedPointCount = 0
            var exportedActivityCount = 0
            val dialect = appSettingsRepository.getSettings().csvExportFormat.resolve(localeProvider())

            normalizedIds.forEachIndexed { index, activityId ->
                coroutineContext.ensureActive()
                val activity = repository.getCachedActivity(activityId)
                if (activity == null) {
                    onProgress(index + 1, normalizedIds.size)
                    return@forEachIndexed
                }

                val detail = repository.getCachedActivityDetail(activityId)
                if (detail == null) {
                    onProgress(index + 1, normalizedIds.size)
                    return@forEachIndexed
                }

                detailRows += buildRows(activity, detail, dialect)
                exportedPointCount += detail.points.size
                exportedActivityCount += 1
                onProgress(index + 1, normalizedIds.size)
            }

            if (exportedActivityCount == 0) {
                error("Keine ausgewählten Aktivitäten mit Detaildaten im Cache verfügbar")
            }

            val timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm", Locale.US))

            BoschActivityDetailsCsvExport(
                fileName = "bosch-activity-details-$timestamp.csv",
                csvContent = buildString {
                    appendLine(dialect.row(CSV_COLUMNS))
                    detailRows.forEach(::appendLine)
                },
                activityCount = exportedActivityCount,
                detailPointCount = exportedPointCount,
            )
        }
    }

    private fun buildRows(
        activity: BoschActivity,
        detail: BoschActivityDetail,
        dialect: CsvDialect,
    ): List<String> = detail.points.mapIndexed { index, point ->
        dialect.row(
            listOf(
                activity.id,
                activity.title,
                dialect.formatIsoDateTime(activity.startTime),
                activity.endTime?.let(dialect::formatIsoDateTime).orEmpty(),
                activity.bikeId.orEmpty(),
                index.toString(),
                point.latitude?.toCsvNumber(dialect).orEmpty(),
                point.longitude?.toCsvNumber(dialect).orEmpty(),
                point.distanceMeters?.toCsvNumber(dialect).orEmpty(),
                point.altitudeMeters?.toCsvNumber(dialect).orEmpty(),
                point.speedKmh?.toCsvNumber(dialect).orEmpty(),
                point.cadenceRpm?.toCsvNumber(dialect).orEmpty(),
                point.riderPowerWatts?.toCsvNumber(dialect).orEmpty(),
            )
        )
    }

    private fun Double.toCsvNumber(dialect: CsvDialect): String = dialect.formatDecimal(this, 6)

    companion object {
        private val CSV_COLUMNS = listOf(
            "activity_id",
            "activity_title",
            "start_time",
            "end_time",
            "bike_id",
            "point_index",
            "latitude",
            "longitude",
            "distance_meters",
            "altitude_meters",
            "speed_kmh",
            "cadence_rpm",
            "rider_power_watts",
        )
    }
}
