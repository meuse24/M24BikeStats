package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.BoschActivitiesCsvExport
import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ExportSmartSystemActivitiesCsvUseCase(
    private val repository: BoschSmartSystemRepository,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(
        onProgress: (loadedCount: Int, totalCount: Int) -> Unit = { _, _ -> },
    ): Result<BoschActivitiesCsvExport> {
        val token = authRepository.getValidAccessToken()
            .getOrElse { return Result.failure(it) }

        val activities = repository.getCachedActivities().toMutableList()
        var total = repository.getCachedActivityTotalCount() ?: activities.size
        var offset = activities.size

        if (activities.isNotEmpty()) {
            onProgress(activities.size, total.coerceAtLeast(activities.size))
        }

        while (activities.size < total) {
            val page = repository.getActivities(
                accessToken = token,
                limit = EXPORT_PAGE_SIZE,
                offset = offset,
            ).getOrElse { return Result.failure(it) }

            total = page.total
            if (page.items.isEmpty()) break

            val knownIds = activities.asSequence().map { it.id }.toHashSet()
            activities += page.items.filterNot { it.id in knownIds }
            onProgress(activities.size, total)
            offset = page.offset + page.items.size
        }

        val timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm", Locale.US))

        return Result.success(
            BoschActivitiesCsvExport(
                fileName = "bosch-activities-$timestamp.csv",
                csvContent = buildCsv(activities),
                activityCount = activities.size,
            )
        )
    }

    private fun buildCsv(activities: List<BoschActivity>): String {
        val rows = buildList {
            add(CSV_HEADER)
            activities.forEach { activity ->
                add(
                    listOf(
                        activity.id,
                        activity.title,
                        activity.startTime,
                        activity.endTime.orEmpty(),
                        activity.timeZone.orEmpty(),
                        activity.durationWithoutStopsSeconds.toString(),
                        activity.bikeId.orEmpty(),
                        activity.startOdometerMeters?.toString().orEmpty(),
                        activity.distanceMeters.toString(),
                        activity.averageSpeedKmh?.toCsvNumber().orEmpty(),
                        activity.maxSpeedKmh?.toCsvNumber().orEmpty(),
                        activity.averageCadenceRpm?.toCsvNumber().orEmpty(),
                        activity.maxCadenceRpm?.toCsvNumber().orEmpty(),
                        activity.averageRiderPowerWatts?.toCsvNumber().orEmpty(),
                        activity.maxRiderPowerWatts?.toCsvNumber().orEmpty(),
                        activity.elevationGainMeters?.toString().orEmpty(),
                        activity.elevationLossMeters?.toString().orEmpty(),
                        activity.caloriesBurned?.toCsvNumber().orEmpty(),
                    ).joinToString(separator = ",") { it.escapeCsv() }
                )
            }
        }

        return rows.joinToString(separator = "\n")
    }

    private fun Double.toCsvNumber(): String = String.format(Locale.US, "%.2f", this)

    private fun String.escapeCsv(): String {
        val escaped = replace("\"", "\"\"")
        return "\"$escaped\""
    }

    companion object {
        private const val EXPORT_PAGE_SIZE = 100
        private const val CSV_HEADER =
            "\"id\",\"title\",\"start_time\",\"end_time\",\"time_zone\",\"duration_without_stops_seconds\",\"bike_id\",\"start_odometer_meters\",\"distance_meters\",\"average_speed_kmh\",\"max_speed_kmh\",\"average_cadence_rpm\",\"max_cadence_rpm\",\"average_rider_power_watts\",\"max_rider_power_watts\",\"elevation_gain_meters\",\"elevation_loss_meters\",\"calories_burned\""
    }
}
