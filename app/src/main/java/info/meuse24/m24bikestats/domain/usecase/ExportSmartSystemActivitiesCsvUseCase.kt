package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.BoschActivitiesCsvExport
import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.model.BoschActivityDetail
import info.meuse24.m24bikestats.domain.model.CsvDialect
import info.meuse24.m24bikestats.domain.repository.AppSettingsRepository
import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

class ExportSmartSystemActivitiesCsvUseCase(
    private val repository: BoschSmartSystemRepository,
    @Suppress("unused")
    private val authRepository: AuthRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val localeProvider: () -> Locale = Locale::getDefault,
) {
    suspend operator fun invoke(
        onProgress: (loadedCount: Int, totalCount: Int) -> Unit = { _, _ -> },
    ): Result<BoschActivitiesCsvExport> = runCatching {
        val activities = repository.getCachedActivities().sortedByDescending { it.startTime }
        if (activities.isEmpty()) {
            error("Keine Aktivitäten im Cache verfügbar")
        }

        val dialect = appSettingsRepository.getSettings().csvExportFormat.resolve(localeProvider())
        val activityRows = activities.mapIndexed { index, activity ->
            coroutineContext.ensureActive()
            val detailAggregate = loadActivityDetailAggregate(
                activityId = activity.id,
            )
            onProgress(index + 1, activities.size)
            ActivityCsvRow(
                activity = activity,
                detailAggregate = detailAggregate,
            )
        }

        val timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm", Locale.US))

        BoschActivitiesCsvExport(
            fileName = "bosch-activities-$timestamp.csv",
            csvContent = buildCsv(
                rows = activityRows,
                dialect = dialect,
            ),
            activityCount = activities.size,
        )
    }

    private fun buildCsv(
        rows: List<ActivityCsvRow>,
        dialect: CsvDialect,
    ): String {
        val csvRows = buildList {
            add(dialect.row(CSV_COLUMNS))
            rows.forEach { row ->
                val activity = row.activity
                val detailAggregate = row.detailAggregate
                add(
                    dialect.row(
                        listOf(
                            activity.id,
                            activity.title,
                            dialect.formatIsoDateTime(activity.startTime),
                            activity.endTime?.let(dialect::formatIsoDateTime).orEmpty(),
                            activity.timeZone.orEmpty(),
                            activity.durationWithoutStopsSeconds.toString(),
                            activity.bikeId.orEmpty(),
                            activity.startOdometerMeters?.toString().orEmpty(),
                            activity.distanceMeters.toString(),
                            activity.averageSpeedKmh?.toCsvNumber(dialect).orEmpty(),
                            activity.maxSpeedKmh?.toCsvNumber(dialect).orEmpty(),
                            activity.averageCadenceRpm?.toCsvNumber(dialect).orEmpty(),
                            activity.maxCadenceRpm?.toCsvNumber(dialect).orEmpty(),
                            activity.averageRiderPowerWatts?.toCsvNumber(dialect).orEmpty(),
                            activity.maxRiderPowerWatts?.toCsvNumber(dialect).orEmpty(),
                            activity.elevationGainMeters?.toString().orEmpty(),
                            activity.elevationLossMeters?.toString().orEmpty(),
                            activity.caloriesBurned?.toCsvNumber(dialect).orEmpty(),
                            detailAggregate.isAvailable.toString(),
                            detailAggregate.pointCount?.toString().orEmpty(),
                            detailAggregate.gpsPointCount?.toString().orEmpty(),
                            detailAggregate.distanceMeters?.toCsvNumber(dialect).orEmpty(),
                            detailAggregate.altitudeMinMeters?.toCsvNumber(dialect).orEmpty(),
                            detailAggregate.altitudeMaxMeters?.toCsvNumber(dialect).orEmpty(),
                            detailAggregate.speedAverageKmh?.toCsvNumber(dialect).orEmpty(),
                            detailAggregate.speedMaxKmh?.toCsvNumber(dialect).orEmpty(),
                            detailAggregate.cadenceAverageRpm?.toCsvNumber(dialect).orEmpty(),
                            detailAggregate.cadenceMaxRpm?.toCsvNumber(dialect).orEmpty(),
                            detailAggregate.riderPowerAverageWatts?.toCsvNumber(dialect).orEmpty(),
                            detailAggregate.riderPowerMaxWatts?.toCsvNumber(dialect).orEmpty(),
                        )
                    )
                )
            }
        }

        return csvRows.joinToString(separator = "\n")
    }

    private suspend fun loadActivityDetailAggregate(
        activityId: String,
    ): ActivityDetailAggregate {
        val detail = repository.getCachedActivityDetail(activityId) ?: return ActivityDetailAggregate.unavailable()
        return detail.toAggregate()
    }

    private fun BoschActivityDetail.toAggregate(): ActivityDetailAggregate {
        val distances = points.mapNotNull { it.distanceMeters }
        val altitudes = points.mapNotNull { it.altitudeMeters }
        val speeds = points.mapNotNull { it.speedKmh }
        val cadences = points.mapNotNull { it.cadenceRpm }
        val riderPowers = points.mapNotNull { it.riderPowerWatts }

        return ActivityDetailAggregate(
            isAvailable = true,
            pointCount = points.size,
            gpsPointCount = points.count { it.latitude != null && it.longitude != null },
            distanceMeters = distances.maxOrNull(),
            altitudeMinMeters = altitudes.minOrNull(),
            altitudeMaxMeters = altitudes.maxOrNull(),
            speedAverageKmh = speeds.averageOrNull(),
            speedMaxKmh = speeds.maxOrNull(),
            cadenceAverageRpm = cadences.averageOrNull(),
            cadenceMaxRpm = cadences.maxOrNull(),
            riderPowerAverageWatts = riderPowers.averageOrNull(),
            riderPowerMaxWatts = riderPowers.maxOrNull(),
        )
    }

    private fun List<Double>.averageOrNull(): Double? =
        if (isEmpty()) null else average()

    private fun Double.toCsvNumber(dialect: CsvDialect): String = dialect.formatDecimal(this, 2)

    private data class ActivityCsvRow(
        val activity: BoschActivity,
        val detailAggregate: ActivityDetailAggregate,
    )

    private data class ActivityDetailAggregate(
        val isAvailable: Boolean,
        val pointCount: Int?,
        val gpsPointCount: Int?,
        val distanceMeters: Double?,
        val altitudeMinMeters: Double?,
        val altitudeMaxMeters: Double?,
        val speedAverageKmh: Double?,
        val speedMaxKmh: Double?,
        val cadenceAverageRpm: Double?,
        val cadenceMaxRpm: Double?,
        val riderPowerAverageWatts: Double?,
        val riderPowerMaxWatts: Double?,
    ) {
        companion object {
            fun unavailable() = ActivityDetailAggregate(
                isAvailable = false,
                pointCount = null,
                gpsPointCount = null,
                distanceMeters = null,
                altitudeMinMeters = null,
                altitudeMaxMeters = null,
                speedAverageKmh = null,
                speedMaxKmh = null,
                cadenceAverageRpm = null,
                cadenceMaxRpm = null,
                riderPowerAverageWatts = null,
                riderPowerMaxWatts = null,
            )
        }
    }

    companion object {
        private val CSV_COLUMNS = listOf(
            "id",
            "title",
            "start_time",
            "end_time",
            "time_zone",
            "duration_without_stops_seconds",
            "bike_id",
            "start_odometer_meters",
            "distance_meters",
            "average_speed_kmh",
            "max_speed_kmh",
            "average_cadence_rpm",
            "max_cadence_rpm",
            "average_rider_power_watts",
            "max_rider_power_watts",
            "elevation_gain_meters",
            "elevation_loss_meters",
            "calories_burned",
            "detail_available",
            "detail_point_count",
            "gps_point_count",
            "track_distance_meters",
            "altitude_min_meters",
            "altitude_max_meters",
            "track_speed_avg_kmh",
            "track_speed_max_kmh",
            "track_cadence_avg_rpm",
            "track_cadence_max_rpm",
            "track_rider_power_avg_watts",
            "track_rider_power_max_watts",
        )
    }
}
