package info.meuse24.m24bikestats.presentation.dashboard

import androidx.annotation.StringRes
import info.meuse24.m24bikestats.R
import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.model.BoschActivityDetail
import info.meuse24.m24bikestats.domain.model.BoschActivityDetailPoint
import info.meuse24.m24bikestats.domain.model.BoschAssistMode
import info.meuse24.m24bikestats.domain.model.BoschBattery
import info.meuse24.m24bikestats.domain.model.BoschBike
import info.meuse24.m24bikestats.domain.model.BoschComponent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class DashboardUiModelMapper(
    private val stringResolver: DashboardStringResolver,
) {
    fun toActivityCardUiModel(activity: BoschActivity): ActivityCardUiModel =
        ActivityCardUiModel(
            id = activity.id,
            title = activity.title,
            startedAt = activity.startTime,
            startedAtEpochMillis = activity.startTime.toEpochMillis(),
            distanceMeters = activity.distanceMeters,
            durationSeconds = activity.durationWithoutStopsSeconds,
            dateLabel = activity.startTime.toReadableDateTime(),
            distanceLabel = activity.distanceMeters.toKilometerText(),
            durationLabel = activity.durationWithoutStopsSeconds.toDurationText(),
            speedLabel = listOfNotNull(
                activity.averageSpeedKmh?.let { s(R.string.dashboard_speed_average, it.toSpeedText()) },
                activity.maxSpeedKmh?.let { s(R.string.dashboard_speed_max, it.toSpeedText()) },
            ).joinToString(" • ").ifBlank { s(R.string.dashboard_no_speed_data) },
            powerLabel = activity.averageRiderPowerWatts?.let { average ->
                activity.maxRiderPowerWatts?.let { maximum ->
                    s(R.string.dashboard_power_average_with_max, average.toWholeNumber(), maximum.toWholeNumber())
                } ?: s(R.string.dashboard_power_average, average.toWholeNumber())
            },
            elevationLabel = if (activity.elevationGainMeters != null && activity.elevationLossMeters != null) {
                s(R.string.dashboard_elevation_balance, activity.elevationGainMeters, activity.elevationLossMeters)
            } else {
                null
            },
            caloriesLabel = activity.caloriesBurned?.let { s(R.string.dashboard_calories_value, it.toWholeNumber()) },
        )

    fun toActivityDetailUiModel(
        activity: BoschActivity,
        detail: BoschActivityDetail,
    ): ActivityDetailUiModel {
        val geoPoints = detail.points.filter { it.hasCoordinates() }
        val speedPoints = detail.points.mapNotNull { it.speedKmh?.takeIf { speed -> speed > 0.0 } }
        val cadencePoints = detail.points.mapNotNull { it.cadenceRpm?.takeIf { cadence -> cadence > 0.0 } }
        val riderPowerPoints = detail.points.mapNotNull { it.riderPowerWatts?.takeIf { power -> power > 0.0 } }
        val altitudePoints = detail.points.mapNotNull { it.altitudeMeters?.takeIf { altitude -> altitude > 0.0 } }
        val lastDistanceMeters = detail.points.lastOrNull { it.distanceMeters != null }?.distanceMeters
        val startCoordinate = geoPoints.firstOrNull()
        val endCoordinate = geoPoints.lastOrNull()
        val trackPoints = geoPoints.mapNotNull { point ->
            val latitude = point.latitude ?: return@mapNotNull null
            val longitude = point.longitude ?: return@mapNotNull null
            ActivityTrackPointUiModel(
                latitude = latitude,
                longitude = longitude,
                altitudeMeters = point.altitudeMeters,
                distanceMeters = point.distanceMeters,
            )
        }
        val profilePoints = detail.points.mapNotNull { point ->
            val distanceMeters = point.distanceMeters ?: return@mapNotNull null
            ActivityProfilePointUiModel(
                distanceMeters = distanceMeters,
                altitudeMeters = point.altitudeMeters?.takeIf { it >= 0.0 && !it.isNaN() },
                speedKmh = point.speedKmh?.takeIf { it >= 0.0 && !it.isNaN() },
                cadenceRpm = point.cadenceRpm?.takeIf { it >= 0.0 && !it.isNaN() },
                riderPowerWatts = point.riderPowerWatts?.takeIf { it >= 0.0 && !it.isNaN() },
            )
        }

        return ActivityDetailUiModel(
            id = activity.id,
            title = activity.title,
            subtitle = s(R.string.dashboard_detail_subtitle, detail.points.size, geoPoints.size),
            summary = buildList {
                add(s(R.string.dashboard_label_start) to activity.startTime.toReadableDateTime())
                add(s(R.string.dashboard_label_distance) to activity.distanceMeters.toKilometerText())
                add(s(R.string.dashboard_label_duration) to activity.durationWithoutStopsSeconds.toDurationText())
                activity.averageSpeedKmh?.let { add(s(R.string.dashboard_label_avg_speed) to it.toSpeedText()) }
            },
            sections = buildList {
                add(
                    DetailSectionUiModel(
                        title = s(R.string.dashboard_section_time_distance),
                        rows = buildList {
                            add(s(R.string.dashboard_label_start) to activity.startTime.toReadableDateTime())
                            activity.endTime?.let { add(s(R.string.dashboard_label_end) to it.toReadableDateTime()) }
                            add(s(R.string.dashboard_label_duration_without_stops) to activity.durationWithoutStopsSeconds.toDurationText())
                            add(s(R.string.dashboard_label_distance) to activity.distanceMeters.toKilometerText())
                            lastDistanceMeters?.let { add(s(R.string.dashboard_label_track_distance) to it.toKilometerText()) }
                            activity.startOdometerMeters?.let { add(s(R.string.dashboard_label_start_odometer) to it.toKilometerText()) }
                            activity.timeZone?.let { add(s(R.string.dashboard_label_time_zone) to it) }
                            activity.bikeId?.let { add(s(R.string.dashboard_label_bike_id) to it) }
                        },
                    )
                )
                add(
                    DetailSectionUiModel(
                        title = s(R.string.dashboard_section_power_ride),
                        rows = buildList {
                            activity.averageSpeedKmh?.let { add(s(R.string.dashboard_label_avg_speed) to it.toSpeedText()) }
                            activity.maxSpeedKmh?.let { add(s(R.string.dashboard_label_max_speed) to it.toSpeedText()) }
                            if (speedPoints.isNotEmpty()) {
                                add(s(R.string.dashboard_label_track_speed_max) to speedPoints.max().toSpeedText())
                            }
                            activity.averageCadenceRpm?.let { add(s(R.string.dashboard_label_avg_cadence) to "${it.toWholeNumber()} rpm") }
                            activity.maxCadenceRpm?.let { add(s(R.string.dashboard_label_max_cadence) to "${it.toWholeNumber()} rpm") }
                            if (cadencePoints.isNotEmpty()) {
                                add(s(R.string.dashboard_label_track_cadence_avg) to "${cadencePoints.average().toWholeNumber()} rpm")
                            }
                            activity.averageRiderPowerWatts?.let { add(s(R.string.dashboard_label_avg_rider_power) to "${it.toWholeNumber()} W") }
                            activity.maxRiderPowerWatts?.let { add(s(R.string.dashboard_label_max_rider_power) to "${it.toWholeNumber()} W") }
                            if (riderPowerPoints.isNotEmpty()) {
                                add(s(R.string.dashboard_label_track_power_avg) to "${riderPowerPoints.average().toWholeNumber()} W")
                            }
                        },
                    )
                )
                add(
                    DetailSectionUiModel(
                        title = s(R.string.dashboard_section_elevation_energy),
                        rows = buildList {
                            if (activity.elevationGainMeters != null && activity.elevationLossMeters != null) {
                                add(
                                    s(R.string.dashboard_label_elevation) to
                                        s(R.string.dashboard_elevation_balance, activity.elevationGainMeters, activity.elevationLossMeters),
                                )
                            }
                            if (altitudePoints.isNotEmpty()) {
                                add(
                                    s(R.string.dashboard_label_elevation_profile) to
                                        s(
                                            R.string.dashboard_elevation_profile_range,
                                            altitudePoints.minOrNull()?.toWholeNumber() ?: 0,
                                            altitudePoints.maxOrNull()?.toWholeNumber() ?: 0,
                                        ),
                                )
                            }
                            activity.caloriesBurned?.let { add(s(R.string.dashboard_label_calories) to s(R.string.dashboard_calories_value, it.toWholeNumber())) }
                        },
                    )
                )
                add(
                    DetailSectionUiModel(
                        title = s(R.string.dashboard_section_track_gps),
                        rows = buildList {
                            add(s(R.string.dashboard_label_detail_points) to detail.points.size.toString())
                            add(s(R.string.dashboard_label_gps_points) to geoPoints.size.toString())
                            startCoordinate?.let {
                                add(s(R.string.dashboard_label_start_coordinate) to "${it.latitude!!.toCoordinateText()}, ${it.longitude!!.toCoordinateText()}")
                            }
                            endCoordinate?.let {
                                add(s(R.string.dashboard_label_end_coordinate) to "${it.latitude!!.toCoordinateText()}, ${it.longitude!!.toCoordinateText()}")
                            }
                        },
                        actions = buildList {
                            add(DetailSectionActionUiModel(label = s(R.string.dashboard_action_share), type = DetailSectionActionType.SHARE))
                            if (trackPoints.isNotEmpty()) {
                                add(DetailSectionActionUiModel(label = s(R.string.dashboard_action_map), type = DetailSectionActionType.MAP))
                            }
                        },
                    )
                )
            }.filter { it.rows.isNotEmpty() },
            trackPoints = trackPoints,
            profilePoints = profilePoints,
        )
    }

    fun toBikeCardUiModel(bike: BoschBike): BikeCardUiModel =
        BikeCardUiModel(
            id = bike.id,
            title = bike.driveUnit?.productName ?: s(R.string.dashboard_bike_fallback_title),
            subtitle = bike.headUnit?.productName,
            odometerLabel = bike.driveUnit?.odometerMeters?.div(1000.0)?.let { String.format(Locale.US, "%.1f km", it) },
            assistSpeedLabel = bike.driveUnit?.maximumAssistanceSpeedKmh?.toSpeedText(),
            batterySummary = bike.batteries.firstOrNull()?.let { battery ->
                battery.totalChargeCycles?.let {
                    s(
                        R.string.dashboard_battery_cycles,
                        battery.productName ?: s(R.string.dashboard_battery_fallback_title),
                        String.format(Locale.US, "%.1f", it),
                    )
                } ?: (battery.productName ?: s(R.string.dashboard_battery_fallback_title))
            },
        )

    fun toBikeDetailUiModel(bike: BoschBike): BikeDetailUiModel =
        BikeDetailUiModel(
            title = bike.driveUnit?.productName ?: s(R.string.dashboard_bike_fallback_title),
            subtitle = bike.headUnit?.productName,
            sections = buildList {
                add(
                    DetailSectionUiModel(
                        title = s(R.string.dashboard_section_overview),
                        rows = buildList {
                            add(s(R.string.dashboard_label_bike_id) to bike.id)
                            bike.createdAt?.let { add(s(R.string.dashboard_label_created_at) to it.toReadableDateTime()) }
                            bike.language?.let { add(s(R.string.dashboard_label_language) to it) }
                            bike.driveUnit?.odometerMeters?.div(1000.0)?.let {
                                add(s(R.string.dashboard_label_odometer) to String.format(Locale.US, "%.1f km", it))
                            }
                            bike.driveUnit?.maximumAssistanceSpeedKmh?.let { add(s(R.string.dashboard_label_max_assist) to it.toSpeedText()) }
                            bike.driveUnit?.rearWheelCircumferenceMillimeters?.let {
                                add(s(R.string.dashboard_label_wheel_circumference) to s(R.string.dashboard_wheel_circumference_value, it.toWholeNumber()))
                            }
                        },
                    )
                )
                bike.driveUnit?.let { driveUnit ->
                    add(
                        DetailSectionUiModel(
                            title = s(R.string.dashboard_section_drive_unit),
                            rows = buildList {
                                driveUnit.productName?.let { add(s(R.string.dashboard_label_product) to it) }
                                driveUnit.partNumber?.let { add(s(R.string.dashboard_label_part_number) to it) }
                                driveUnit.serialNumber?.let { add(s(R.string.dashboard_label_serial_number) to it) }
                                driveUnit.walkAssistEnabled?.let {
                                    add(s(R.string.dashboard_label_walk_assist) to if (it) s(R.string.dashboard_walk_assist_active) else s(R.string.dashboard_walk_assist_inactive))
                                }
                                driveUnit.walkAssistMaximumSpeedKmh?.let { add(s(R.string.dashboard_label_walk_assist_max) to it.toSpeedText()) }
                                driveUnit.totalPowerOnHours?.let { add(s(R.string.dashboard_label_total_power_on_hours) to s(R.string.dashboard_hours_value, it)) }
                                driveUnit.supportPowerOnHours?.let { add(s(R.string.dashboard_label_support_power_on_hours) to s(R.string.dashboard_hours_value, it)) }
                                if (driveUnit.activeAssistModes.isNotEmpty()) {
                                    add(s(R.string.dashboard_label_assist_modes) to driveUnit.activeAssistModes.toAssistModeSummary())
                                }
                            },
                        )
                    )
                }
                if (bike.batteries.isNotEmpty()) {
                    add(
                        DetailSectionUiModel(
                            title = s(R.string.dashboard_section_batteries),
                            rows = bike.batteries.flatMapIndexed { index, battery ->
                                battery.toRows(prefix = s(R.string.dashboard_battery_prefix, index + 1))
                            },
                        )
                    )
                }
                bike.remoteControl?.let { add(DetailSectionUiModel(title = s(R.string.dashboard_section_remote), rows = it.toRows())) }
                bike.headUnit?.let { add(DetailSectionUiModel(title = s(R.string.dashboard_section_head_unit), rows = it.toRows())) }
            },
        )

    private fun BoschBattery.toRows(prefix: String): List<Pair<String, String>> = buildList {
        productName?.let { add(s(R.string.dashboard_battery_prefix_product, prefix) to it) }
        partNumber?.let { add(s(R.string.dashboard_battery_prefix_part_number, prefix) to it) }
        serialNumber?.let { add(s(R.string.dashboard_battery_prefix_serial_number, prefix) to it) }
        deliveredWhOverLifetime?.let { add(s(R.string.dashboard_battery_prefix_energy, prefix) to s(R.string.dashboard_wh_value, it)) }
        totalChargeCycles?.let { add(s(R.string.dashboard_battery_prefix_total_cycles, prefix) to s(R.string.dashboard_cycles_value, String.format(Locale.US, "%.1f", it))) }
        onBikeChargeCycles?.let { add(s(R.string.dashboard_battery_prefix_on_bike_cycles, prefix) to s(R.string.dashboard_cycles_value, String.format(Locale.US, "%.1f", it))) }
        offBikeChargeCycles?.let { add(s(R.string.dashboard_battery_prefix_off_bike_cycles, prefix) to s(R.string.dashboard_cycles_value, String.format(Locale.US, "%.1f", it))) }
    }

    private fun BoschComponent.toRows(): List<Pair<String, String>> = buildList {
        productName?.let { add(s(R.string.dashboard_label_product) to it) }
        partNumber?.let { add(s(R.string.dashboard_label_part_number) to it) }
        serialNumber?.let { add(s(R.string.dashboard_label_serial_number) to it) }
    }

    private fun List<BoschAssistMode>.toAssistModeSummary(): String =
        joinToString(" | ") { mode ->
            mode.reachableRangeKm?.let { s(R.string.dashboard_assist_mode_range, mode.name, it.toWholeNumber()) } ?: mode.name
        }

    private fun String.toReadableDateTime(): String =
        runCatching {
            Instant.parse(this)
                .atZone(ZoneId.systemDefault())
                .format(DATE_TIME_FORMATTER)
        }.getOrDefault(this)

    private fun String.toEpochMillis(): Long? =
        runCatching { Instant.parse(this).toEpochMilli() }.getOrNull()

    private fun Int.toDurationText(): String {
        val hours = this / 3600
        val minutes = (this % 3600) / 60
        return when {
            hours > 0 -> s(R.string.dashboard_duration_hours_minutes, hours, minutes)
            else -> s(R.string.dashboard_duration_minutes, minutes)
        }
    }

    private fun Int.toKilometerText(): String =
        String.format(Locale.US, "%.1f km", this / 1000.0)

    private fun Double.toKilometerText(): String =
        String.format(Locale.US, "%.1f km", this / 1000.0)

    private fun Double.toSpeedText(): String =
        String.format(Locale.US, "%.1f km/h", this)

    private fun Double.toWholeNumber(): String =
        String.format(Locale.US, "%.0f", this)

    private fun Double.toCoordinateText(): String =
        String.format(Locale.US, "%.5f", this)

    private fun BoschActivityDetailPoint.hasCoordinates(): Boolean {
        val latitude = latitude ?: return false
        val longitude = longitude ?: return false
        return latitude != 0.0 || longitude != 0.0
    }

    private fun s(@StringRes resId: Int, vararg args: Any): String =
        stringResolver.get(resId, args)

    private companion object {
        private val DATE_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    }
}
