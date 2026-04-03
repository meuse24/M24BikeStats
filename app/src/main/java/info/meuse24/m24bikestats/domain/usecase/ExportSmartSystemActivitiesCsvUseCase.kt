package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.BoschActivitiesCsvExport
import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.model.CsvDialect
import info.meuse24.m24bikestats.domain.repository.AppSettingsRepository
import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ExportSmartSystemActivitiesCsvUseCase(
    private val repository: BoschSmartSystemRepository,
    private val authRepository: AuthRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val localeProvider: () -> Locale = Locale::getDefault,
) {
    suspend operator fun invoke(
        onProgress: (loadedCount: Int, totalCount: Int) -> Unit = { _, _ -> },
    ): Result<BoschActivitiesCsvExport> = withValidAccessToken(authRepository) { token ->
        val activities = repository.getCachedActivities().toMutableList()
        var total = repository.getCachedActivityTotalCount()
        var offset = activities.size

        if (activities.isEmpty() && (total == null || total == 0)) {
            val firstPage = repository.getActivities(
                accessToken = token,
                limit = EXPORT_PAGE_SIZE,
                offset = 0,
            ).getOrElse { return@withValidAccessToken Result.failure(it) }

            total = firstPage.total
            activities += firstPage.items
            offset = firstPage.offset + firstPage.items.size
            onProgress(activities.size, total.coerceAtLeast(activities.size))
        }

        var totalCount = total ?: activities.size

        if (activities.isNotEmpty()) {
            onProgress(activities.size, totalCount.coerceAtLeast(activities.size))
        }

        while (activities.size < totalCount) {
            val page = repository.getActivities(
                accessToken = token,
                limit = EXPORT_PAGE_SIZE,
                offset = offset,
            ).getOrElse { return@withValidAccessToken Result.failure(it) }

            totalCount = page.total
            if (page.items.isEmpty()) break

            val knownIds = activities.asSequence().map { it.id }.toHashSet()
            activities += page.items.filterNot { it.id in knownIds }
            onProgress(activities.size, totalCount)
            offset = page.offset + page.items.size
        }

        val timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm", Locale.US))

        Result.success(
            BoschActivitiesCsvExport(
                fileName = "bosch-activities-$timestamp.csv",
                csvContent = buildCsv(
                    activities = activities,
                    dialect = appSettingsRepository.getSettings().csvExportFormat.resolve(localeProvider()),
                ),
                activityCount = activities.size,
            )
        )
    }

    private fun buildCsv(
        activities: List<BoschActivity>,
        dialect: CsvDialect,
    ): String {
        val rows = buildList {
            add(dialect.row(CSV_COLUMNS))
            activities.forEach { activity ->
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
                        )
                    )
                )
            }
        }

        return rows.joinToString(separator = "\n")
    }

    private fun Double.toCsvNumber(dialect: CsvDialect): String = dialect.formatDecimal(this, 2)

    companion object {
        private const val EXPORT_PAGE_SIZE = 100
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
        )
    }
}
