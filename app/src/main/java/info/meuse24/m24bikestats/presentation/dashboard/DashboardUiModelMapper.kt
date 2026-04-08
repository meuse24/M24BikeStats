package info.meuse24.m24bikestats.presentation.dashboard

import androidx.annotation.StringRes
import info.meuse24.m24bikestats.auth.OidcDiscoveryInfoUiModel
import info.meuse24.m24bikestats.auth.OidcCertificateInfoUiModel
import info.meuse24.m24bikestats.auth.OidcUserInfoUiModel
import info.meuse24.m24bikestats.R
import info.meuse24.m24bikestats.domain.model.DataStatusOverview
import info.meuse24.m24bikestats.domain.model.DataStatusState
import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.model.BoschActivityDetail
import info.meuse24.m24bikestats.domain.model.BoschActivityDetailPoint
import info.meuse24.m24bikestats.domain.model.BoschAssistMode
import info.meuse24.m24bikestats.domain.model.BoschBattery
import info.meuse24.m24bikestats.domain.model.BoschBatteryCycleRisk
import info.meuse24.m24bikestats.domain.model.BoschBatteryHealth
import info.meuse24.m24bikestats.domain.model.BoschBatteryHealthBand
import info.meuse24.m24bikestats.domain.model.BoschBatteryStressLevel
import info.meuse24.m24bikestats.domain.model.BoschBike
import info.meuse24.m24bikestats.domain.model.BoschComponent
import info.meuse24.m24bikestats.domain.model.BoschDriveUnit
import info.meuse24.m24bikestats.domain.model.BoschRegistration
import info.meuse24.m24bikestats.domain.model.BoschTheftReportLog
import info.meuse24.m24bikestats.domain.model.BoschServiceRecord
import info.meuse24.m24bikestats.domain.model.estimateHealth
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class DashboardUiModelMapper(
    private val stringResolver: DashboardStringResolver,
) {
    fun toDataStatusUiModel(overview: DataStatusOverview): DataStatusUiModel {
        val detailCoveragePercent = if (overview.cachedActivityCount > 0) {
            (overview.detailedActivityCount.toDouble() / overview.cachedActivityCount.toDouble() * 100.0).roundToInt()
        } else {
            0
        }
        return DataStatusUiModel(
            statusTone = when (overview.status) {
                DataStatusState.EMPTY -> DataStatusTone.EMPTY
                DataStatusState.PARTIAL -> DataStatusTone.PARTIAL
                DataStatusState.COMPLETE -> DataStatusTone.COMPLETE
            },
            statusLabel = when (overview.status) {
                DataStatusState.EMPTY -> s(R.string.home_data_status_state_empty)
                DataStatusState.PARTIAL -> s(R.string.home_data_status_state_partial)
                DataStatusState.COMPLETE -> s(R.string.home_data_status_state_complete)
            },
            statusSummary = when {
                overview.status == DataStatusState.EMPTY ->
                    s(R.string.home_data_status_summary_empty)
                overview.missingDetailCount > 0 ->
                    if (overview.staleDetailCount > 0) {
                        s(
                            R.string.home_data_status_summary_partial_stale,
                            overview.missingDetailCount,
                            overview.staleDetailCount,
                        )
                    } else {
                        s(R.string.home_data_status_summary_partial, overview.missingDetailCount)
                    }
                overview.staleDetailCount > 0 ->
                    s(R.string.home_data_status_summary_stale, overview.staleDetailCount)
                else ->
                    s(R.string.home_data_status_summary_complete)
            },
            coveredPeriodLabel = overview.coveredActivityStartEpochMillis?.let { startEpochMillis ->
                overview.coveredActivityEndEpochMillis?.let { endEpochMillis ->
                    startEpochMillis.toReadableDateRange(endEpochMillis)
                }
            },
            cachedActivityCount = overview.cachedActivityCount,
            detailedActivityCount = overview.detailedActivityCount,
            detailCoverageLabel = s(
                R.string.home_data_status_detail_coverage,
                overview.detailedActivityCount,
                overview.cachedActivityCount,
                detailCoveragePercent,
            ),
            missingDetailCount = overview.missingDetailCount,
            staleDetailCount = overview.staleDetailCount,
            gpsPointCount = overview.gpsPointCount,
            lastActivitySyncLabel = overview.lastActivitySyncAtEpochMillis?.toReadableDateTime(),
            lastBikeSyncLabel = overview.lastBikeSyncAtEpochMillis?.toReadableDateTime(),
            lastDetailSyncLabel = overview.lastDetailSyncAtEpochMillis?.toReadableDateTime(),
        )
    }

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
        val trackPoints = detail.points.toTrackPoints()
        val profilePoints = detail.points.toProfilePoints()
        val speedPoints = profilePoints.mapNotNull { it.speedKmh?.takeIf { speed -> speed > 0.0 } }
        val cadencePoints = profilePoints.mapNotNull { it.cadenceRpm?.takeIf { cadence -> cadence > 0.0 } }
        val riderPowerPoints = profilePoints.mapNotNull { it.riderPowerWatts?.takeIf { power -> power > 0.0 } }
        val altitudePoints = profilePoints.mapNotNull { it.altitudeMeters?.takeIf { altitude -> altitude > 0.0 } }
        val lastDistanceMeters = profilePoints.lastOrNull()?.distanceMeters
        val startCoordinate = trackPoints.firstOrNull()
        val endCoordinate = trackPoints.lastOrNull()

        return ActivityDetailUiModel(
            id = activity.id,
            title = activity.title,
            subtitle = s(R.string.dashboard_detail_subtitle, detail.points.size, trackPoints.size),
            overview = toActivityCardUiModel(activity),
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
                            add(s(R.string.dashboard_label_gps_points) to trackPoints.size.toString())
                            startCoordinate?.let {
                                add(s(R.string.dashboard_label_start_coordinate) to "${it.latitude.toCoordinateText()}, ${it.longitude.toCoordinateText()}")
                            }
                            endCoordinate?.let {
                                add(s(R.string.dashboard_label_end_coordinate) to "${it.latitude.toCoordinateText()}, ${it.longitude.toCoordinateText()}")
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
        bike.run {
            val driveUnit = driveUnit
            val displayAssistModes = driveUnit?.activeAssistModes.orEmpty().toDisplayAssistModes()
            val detailUiModel = toBikeDetailUiModel(this, oidcCertificateInfo = null)
            BikeCardUiModel(
                id = id,
                title = driveUnit?.productName ?: s(R.string.dashboard_bike_fallback_title),
                subtitle = headUnit?.productName,
                odometerLabel = driveUnit?.odometerMeters?.div(1000.0)?.let { String.format(Locale.US, "%.1f km", it) },
                assistSpeedLabel = driveUnit?.maximumAssistanceSpeedKmh?.toSpeedText(),
                walkAssistLabel = driveUnit?.walkAssistEnabled?.let {
                    if (it) s(R.string.dashboard_walk_assist_active) else s(R.string.dashboard_walk_assist_inactive)
                },
                powerOnSummary = driveUnit?.toPowerOnSummary(),
                assistModesSummary = displayAssistModes.toAssistModeRangeSummary(),
                batterySummary = batteries.firstOrNull()?.toBikeCardSummary(driveUnit),
                bikePassSummary = bikePass.toBikePassCardSummary(theftReportLogs.size),
                shareText = detailUiModel.toShareText(),
            )
        }

    fun toBikeDetailUiModel(
        bike: BoschBike,
        oidcCertificateInfo: OidcCertificateInfoUiModel? = null,
        oidcUserInfo: OidcUserInfoUiModel? = null,
        oidcDiscoveryInfo: OidcDiscoveryInfoUiModel? = null,
    ): BikeDetailUiModel =
        bike.run {
            val driveUnit = driveUnit
            val displayAssistModes = driveUnit?.activeAssistModes.orEmpty().toDisplayAssistModes()
            val accountProfileRows = oidcUserInfo?.toAccountProfileRows().orEmpty()
            val oidcSections = buildList {
                if (accountProfileRows.isNotEmpty()) {
                    add(
                        DetailSectionUiModel(
                            title = s(R.string.dashboard_section_oidc_userinfo),
                            rows = accountProfileRows,
                        ),
                    )
                }
                oidcDiscoveryInfo?.let { discoveryInfo ->
                    add(
                        DetailSectionUiModel(
                            title = s(R.string.dashboard_section_oidc_discovery),
                            rows = buildList {
                                discoveryInfo.issuer?.let { add(s(R.string.dashboard_label_oidc_discovery_issuer) to it) }
                                discoveryInfo.authorizationEndpoint?.let { add(s(R.string.dashboard_label_oidc_authorization_endpoint) to it) }
                                discoveryInfo.tokenEndpoint?.let { add(s(R.string.dashboard_label_oidc_token_endpoint) to it) }
                                discoveryInfo.userInfoEndpoint?.let { add(s(R.string.dashboard_label_oidc_userinfo_endpoint) to it) }
                                discoveryInfo.jwksUri?.let { add(s(R.string.dashboard_label_oidc_jwks_uri) to it) }
                                discoveryInfo.revocationEndpoint?.let { add(s(R.string.dashboard_label_oidc_revocation_endpoint) to it) }
                                discoveryInfo.introspectionEndpoint?.let { add(s(R.string.dashboard_label_oidc_introspection_endpoint) to it) }
                                discoveryInfo.endSessionEndpoint?.let { add(s(R.string.dashboard_label_oidc_end_session_endpoint) to it) }
                                discoveryInfo.supportedGrantTypes
                                    .takeIf { it.isNotEmpty() }
                                    ?.let { add(s(R.string.dashboard_label_oidc_supported_grants) to it.joinToString()) }
                            },
                        ),
                    )
                }
                oidcCertificateInfo?.let { certificateInfo ->
                    add(
                        DetailSectionUiModel(
                            title = s(R.string.dashboard_section_oidc_certificate),
                            rows = buildList {
                                certificateInfo.tokenKeyId?.let { add(s(R.string.dashboard_label_token_key_id) to it) }
                                add(s(R.string.dashboard_label_oidc_key_id) to certificateInfo.keyId)
                                add(
                                    s(R.string.dashboard_label_oidc_key_match) to
                                        if (certificateInfo.matchesCurrentToken) {
                                            s(R.string.dashboard_value_yes)
                                        } else {
                                            s(R.string.dashboard_value_no)
                                        },
                                )
                                certificateInfo.keyType?.let { add(s(R.string.dashboard_label_oidc_key_type) to it) }
                                certificateInfo.algorithm?.let { add(s(R.string.dashboard_label_oidc_algorithm) to it) }
                                certificateInfo.usage?.let { add(s(R.string.dashboard_label_oidc_usage) to it) }
                                certificateInfo.subject?.let { add(s(R.string.dashboard_label_oidc_subject) to it) }
                                certificateInfo.issuer?.let { add(s(R.string.dashboard_label_oidc_issuer) to it) }
                                certificateInfo.validFrom?.let { add(s(R.string.dashboard_label_oidc_valid_from) to it) }
                                certificateInfo.validUntil?.let { add(s(R.string.dashboard_label_oidc_valid_until) to it) }
                                certificateInfo.sha1Thumbprint?.let { add(s(R.string.dashboard_label_oidc_thumbprint_sha1) to it) }
                                certificateInfo.sha256Thumbprint?.let { add(s(R.string.dashboard_label_oidc_thumbprint_sha256) to it) }
                                add(s(R.string.dashboard_label_oidc_chain_entries) to certificateInfo.certificateChainEntries.toString())
                            },
                        ),
                    )
                }
            }
            BikeDetailUiModel(
                title = driveUnit?.productName ?: s(R.string.dashboard_bike_fallback_title),
                subtitle = headUnit?.productName,
                sections = buildList {
                    if (accountProfileRows.isNotEmpty()) {
                        add(
                            DetailSectionUiModel(
                                title = s(R.string.pdf_section_account),
                                rows = accountProfileRows,
                            ),
                        )
                    }
                    add(
                        DetailSectionUiModel(
                            title = s(R.string.dashboard_section_system_support),
                            rows = buildList {
                                add(s(R.string.dashboard_label_supported_platform) to s(R.string.dashboard_value_supported_platform))
                            },
                        ),
                    )
                    add(
                        DetailSectionUiModel(
                            title = s(R.string.dashboard_section_overview),
                            rows = buildList {
                                add(s(R.string.dashboard_label_bike_id) to id)
                                createdAt?.let { add(s(R.string.dashboard_label_created_at) to it.toReadableDateTime()) }
                                language?.let { add(s(R.string.dashboard_label_language) to it) }
                                oemId?.let { add(s(R.string.dashboard_label_oem_id) to it) }
                                driveUnit?.odometerMeters?.div(1000.0)?.let {
                                    add(s(R.string.dashboard_label_odometer) to String.format(Locale.US, "%.1f km", it))
                                }
                                driveUnit?.maximumAssistanceSpeedKmh?.let { add(s(R.string.dashboard_label_max_assist) to it.toSpeedText()) }
                                driveUnit?.rearWheelCircumferenceMillimeters?.let {
                                    add(s(R.string.dashboard_label_wheel_circumference) to s(R.string.dashboard_wheel_circumference_value, it.toWholeNumber()))
                                }
                                serviceDueDate?.let { add(s(R.string.dashboard_label_service_due_date) to it.toReadableDateTime()) }
                                serviceDueOdometerMeters?.let {
                                    add(s(R.string.dashboard_label_service_due_odometer) to it.toKilometerText())
                                }
                            },
                        ),
                    )
                    driveUnit?.let { driveUnit ->
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
                                },
                            ),
                        )
                    }
                    if (displayAssistModes.isNotEmpty()) {
                        add(
                            DetailSectionUiModel(
                                title = s(R.string.dashboard_section_assist_modes),
                                rows = displayAssistModes.map { mode ->
                                    mode.name to (mode.reachableRangeKm?.let { s(R.string.dashboard_assist_range_value, it.toWholeNumber()) }
                                        ?: s(R.string.dashboard_assist_mode_no_range))
                                },
                            ),
                        )
                    }
                    if (batteries.isNotEmpty()) {
                        addAll(
                            batteries.mapIndexed { index, battery ->
                                battery.toDetailSection(
                                    title = s(R.string.dashboard_battery_prefix, index + 1),
                                    driveUnit = driveUnit,
                                )
                            },
                        )
                    }
                    remoteControl?.let { add(DetailSectionUiModel(title = s(R.string.dashboard_section_remote), rows = it.toRows())) }
                    headUnit?.let { add(DetailSectionUiModel(title = s(R.string.dashboard_section_head_unit), rows = it.toRows())) }
                    connectModule?.let { add(DetailSectionUiModel(title = s(R.string.dashboard_section_connect_module), rows = it.toRows())) }
                    if (antiLockBrakeSystems.isNotEmpty()) {
                        addAll(
                            antiLockBrakeSystems.mapIndexed { index, component ->
                                DetailSectionUiModel(
                                    title = s(R.string.dashboard_section_abs_component, index + 1),
                                    rows = component.toRows(),
                                )
                            }
                        )
                    }
                    bikePass?.let { bikePass ->
                        add(
                            DetailSectionUiModel(
                                title = s(R.string.dashboard_section_bike_pass),
                                rows = buildList {
                                    bikePass.frameNumber?.let { add(s(R.string.dashboard_label_frame_number) to it) }
                                    bikePass.frameNumberPosition?.let { add(s(R.string.dashboard_label_frame_number_position) to it) }
                                    bikePass.description?.let { add(s(R.string.dashboard_label_bike_pass_description) to it) }
                                    bikePass.createdAt?.let { add(s(R.string.dashboard_label_created_at) to it.toReadableDateTime()) }
                                    bikePass.updatedAt?.let { add(s(R.string.dashboard_label_updated_at) to it.toReadableDateTime()) }
                                },
                            )
                        )
                    }
                    if (theftReportLogs.isNotEmpty()) {
                        addAll(theftReportLogs.mapIndexed { index, log -> log.toDetailSection(index) })
                    }
                    add(
                        DetailSectionUiModel(
                            title = s(R.string.dashboard_section_service_book),
                            rows = buildList {
                                add(s(R.string.dashboard_label_records) to serviceRecords.size.toString())
                                serviceRecords.firstOrNull()?.type?.let { add(s(R.string.dashboard_label_type) to it) }
                                serviceRecords.firstOrNull()?.createdAt?.let {
                                    add(s(R.string.dashboard_label_created_at) to it.toReadableDateTime())
                                }
                            },
                        )
                    )
                    if (serviceRecords.isNotEmpty()) {
                        addAll(serviceRecords.mapIndexed { index, record -> record.toDetailSection(index) })
                    }
                    if (registrations.isNotEmpty()) {
                        add(
                            DetailSectionUiModel(
                                title = s(R.string.dashboard_section_registrations),
                                rows = buildList {
                                    add(s(R.string.dashboard_label_records) to registrations.size.toString())
                                    registrations.firstOrNull { it.registrationType == "BIKE_REGISTRATION" }?.createdAt?.let {
                                        add(s(R.string.dashboard_label_bike_registered_at) to it.toReadableDateTime())
                                    }
                                    registrations
                                        .filter { it.registrationType == "COMPONENT_REGISTRATION" }
                                        .takeIf { it.isNotEmpty() }
                                        ?.let { componentRegistrations ->
                                            add(
                                                s(R.string.dashboard_label_registered_components) to
                                                    componentRegistrations.joinToString { it.componentType ?: s(R.string.pdf_value_not_available) },
                                            )
                                        }
                                },
                            )
                        )
                        addAll(
                            registrations
                                .filter { it.registrationType == "COMPONENT_REGISTRATION" }
                                .mapIndexed { index, registration -> registration.toDetailSection(index) }
                        )
                    }
                    addAll(oidcSections)
                }.filter { it.rows.isNotEmpty() },
            )
        }

    private fun BoschBattery.toBikeCardSummary(driveUnit: BoschDriveUnit?): String {
        val health = estimateHealth(driveUnit)
        val name = productName ?: s(R.string.dashboard_battery_fallback_title)
        return when {
            health.healthPercent != null && totalChargeCycles != null -> s(
                R.string.dashboard_battery_health_cycles,
                name,
                health.healthPercent,
                String.format(Locale.US, "%.1f", totalChargeCycles),
            )

            health.healthPercent != null -> s(
                R.string.dashboard_battery_health_summary,
                name,
                health.healthPercent,
            )

            totalChargeCycles != null -> s(
                R.string.dashboard_battery_cycles,
                name,
                String.format(Locale.US, "%.1f", totalChargeCycles),
            )

            else -> name
        }
    }

    private fun info.meuse24.m24bikestats.domain.model.BoschBikePass?.toBikePassCardSummary(
        theftReportCount: Int,
    ): String? {
        if (this == null && theftReportCount <= 0) return null

        val summaryParts = buildList {
            this@toBikePassCardSummary?.frameNumber?.let { frameNumber ->
                add(s(R.string.dashboard_label_frame_number) + ": " + frameNumber)
            }
            if (theftReportCount > 0) {
                add(s(R.string.dashboard_label_records) + ": " + theftReportCount)
            }
        }

        return summaryParts.joinToString(" • ").ifBlank { null }
    }

    private fun BoschBattery.toDetailSection(
        title: String,
        driveUnit: BoschDriveUnit?,
    ): DetailSectionUiModel {
        val health = estimateHealth(driveUnit)
        return DetailSectionUiModel(
            title = title,
            rows = buildList {
                productName?.let { add(s(R.string.dashboard_label_product) to it) }
                health.healthPercent?.let {
                    add(s(R.string.dashboard_label_battery_health) to "${it} % • ${health.healthBand.toLabel()}")
                }
                health.cycleRisk?.let { add(s(R.string.dashboard_label_battery_cycle_risk) to it.toLabel()) }
                health.nominalCapacityWh?.let { add(s(R.string.dashboard_label_battery_nominal_capacity) to s(R.string.dashboard_wh_value, it)) }
                health.deliveredWhPerSupportHour?.let {
                    add(
                        s(R.string.dashboard_label_battery_stress) to
                            s(
                                R.string.dashboard_battery_stress_value,
                                it.toWholeNumber(),
                                health.stressLevel.toLabel(),
                            ),
                    )
                }
                partNumber?.let { add(s(R.string.dashboard_label_part_number) to it) }
                serialNumber?.let { add(s(R.string.dashboard_label_serial_number) to it) }
                deliveredWhOverLifetime?.let { add(s(R.string.dashboard_label_delivered_energy) to s(R.string.dashboard_wh_value, it)) }
                totalChargeCycles?.let {
                    add(
                        s(R.string.dashboard_label_total_charge_cycles) to
                            s(R.string.dashboard_cycles_value, String.format(Locale.US, "%.1f", it)),
                    )
                }
                onBikeChargeCycles?.let {
                    add(
                        s(R.string.dashboard_label_on_bike_cycles) to
                            s(R.string.dashboard_cycles_value, String.format(Locale.US, "%.1f", it)),
                    )
                }
                offBikeChargeCycles?.let {
                    add(
                        s(R.string.dashboard_label_off_bike_cycles) to
                            s(R.string.dashboard_cycles_value, String.format(Locale.US, "%.1f", it)),
                    )
                }
            },
            indicator = health.toIndicator(),
        )
    }

    private fun BoschBatteryHealth.toIndicator(): DetailSectionIndicatorUiModel? {
        val percent = healthPercent ?: return null
        return DetailSectionIndicatorUiModel(
            label = s(R.string.dashboard_label_battery_health),
            value = "$percent %",
            progress = progress ?: 0f,
            tone = healthBand.toIndicatorTone(),
            supportingText = listOfNotNull(
                healthBand?.toLabel(),
                cycleRisk?.let { "${s(R.string.dashboard_label_battery_cycle_risk)} ${it.toLabel()}" },
                stressLevel?.let { "${s(R.string.dashboard_label_battery_stress_short)} ${it.toLabel()}" },
            ).joinToString(" • "),
        )
    }

    private fun BoschComponent.toRows(): List<Pair<String, String>> = buildList {
        productName?.let { add(s(R.string.dashboard_label_product) to it) }
        partNumber?.let { add(s(R.string.dashboard_label_part_number) to it) }
        serialNumber?.let { add(s(R.string.dashboard_label_serial_number) to it) }
    }

    private fun BoschTheftReportLog.toDetailSection(index: Int): DetailSectionUiModel =
        DetailSectionUiModel(
            title = s(R.string.dashboard_section_theft_report, index + 1),
            rows = buildList {
                createdAt?.let { add(s(R.string.dashboard_label_created_at) to it.toReadableDateTime()) }
                theftCaseEnteredAt?.let { add(s(R.string.dashboard_label_theft_case_entered) to it.toReadableDateTime()) }
                description?.let { add(s(R.string.dashboard_label_bike_pass_description) to it) }
                riderPortalLink?.let { add(s(R.string.dashboard_label_portal_link) to it) }
                timeZone?.let { add(s(R.string.dashboard_label_time_zone) to it) }
                expiresAtEpochMillis?.let {
                    add(
                        s(R.string.dashboard_label_expires_at) to
                            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DATE_TIME_FORMATTER),
                    )
                }
                location?.address?.let { add(s(R.string.dashboard_label_location_address) to it) }
                location?.description?.let { add(s(R.string.dashboard_label_location_note) to it) }
                if (location?.latitude != null && location.longitude != null) {
                    add(
                        s(R.string.dashboard_label_location_coordinates) to
                            "${location.latitude.toCoordinateText()}, ${location.longitude.toCoordinateText()}",
                    )
                }
                location?.horizontalAccuracyMeters?.let {
                    add(s(R.string.dashboard_label_location_accuracy) to s(R.string.dashboard_meters_value, it.toWholeNumber()))
                }
                location?.detectedAt?.let { add(s(R.string.dashboard_label_location_detected_at) to it.toReadableDateTime()) }
            },
        )

    private fun BoschServiceRecord.toDetailSection(index: Int): DetailSectionUiModel =
        DetailSectionUiModel(
            title = s(R.string.dashboard_section_service_record, index + 1),
            rows = buildList {
                add(s(R.string.dashboard_label_type) to type)
                add(s(R.string.dashboard_label_created_at) to createdAt.toReadableDateTime())
                odometerValueMeters?.let { add(s(R.string.dashboard_label_odometer) to it.toDouble().toKilometerText()) }
                bikeDealerName?.let { dealer ->
                    val value = listOfNotNull(dealer, bikeDealerCity).joinToString(", ")
                    add(s(R.string.dashboard_label_bike_dealer) to value)
                }
                toolVersion?.let { add(s(R.string.dashboard_label_tool_version) to it) }
                batteryMeasurement?.measuredCapacityPercentage?.let {
                    add(s(R.string.dashboard_label_battery_health) to "$it %")
                }
                batteryMeasurement?.measuredEnergyCapacityWh?.let {
                    add(s(R.string.dashboard_label_measured_capacity) to s(R.string.dashboard_wh_value, it))
                }
                batteryMeasurement?.nominalEnergyCapacityWh?.let {
                    add(s(R.string.dashboard_label_nominal_capacity_short) to s(R.string.dashboard_wh_value, it))
                }
                batteryMeasurement?.fullChargeCycles?.let {
                    add(s(R.string.dashboard_label_total_charge_cycles) to it.toString())
                }
                softwareUpdate?.let { update ->
                    val client = listOfNotNull(update.clientType, update.clientVersion).joinToString(" ")
                    if (client.isNotBlank()) add(s(R.string.dashboard_label_update_client) to client)
                    add(
                        s(R.string.dashboard_label_updated_components) to
                            if (update.updatedComponentNames.isNotEmpty()) {
                                "${update.updatedComponentsCount}: ${update.updatedComponentNames.joinToString()}"
                            } else {
                                update.updatedComponentsCount.toString()
                            }
                    )
                    update.isForcedUpdate?.let {
                        add(s(R.string.dashboard_label_forced_update) to if (it) s(R.string.dashboard_value_yes) else s(R.string.dashboard_value_no))
                    }
                }
            },
        )

    private fun BoschRegistration.toDetailSection(index: Int): DetailSectionUiModel =
        DetailSectionUiModel(
            title = s(R.string.dashboard_section_registration_component, index + 1),
            rows = buildList {
                componentType?.let { add(s(R.string.dashboard_label_component_type) to it) }
                add(s(R.string.dashboard_label_created_at) to createdAt.toReadableDateTime())
                partNumber?.let { add(s(R.string.dashboard_label_part_number) to it) }
                serialNumber?.let { add(s(R.string.dashboard_label_serial_number) to it) }
            },
        )

    private fun OidcUserInfoUiModel.toAccountProfileRows(): List<Pair<String, String>> =
        buildList {
            email?.let { add(s(R.string.dashboard_label_oidc_user_email) to it) }
            username?.let { add(s(R.string.dashboard_label_oidc_user_username) to it) }
            subject?.let { add(s(R.string.dashboard_label_oidc_user_subject) to it) }
        }

    private fun BikeDetailUiModel.toShareText(): String =
        buildString {
            appendLine(title)
            subtitle?.takeIf { it.isNotBlank() }?.let { appendLine(it) }

            sections.forEach { section ->
                if (section.rows.isEmpty()) return@forEach
                appendLine()
                appendLine(section.title)
                section.rows.forEach { (label, value) ->
                    appendLine("$label: $value")
                }
            }
        }.trim()

    private fun List<BoschAssistMode>.toDisplayAssistModes(): List<BoschAssistMode> =
        filterNot { mode ->
            mode.name == "0" && ((mode.reachableRangeKm ?: 0.0) <= 0.0)
        }.filter { mode ->
            mode.name.isNotBlank() || ((mode.reachableRangeKm ?: 0.0) > 0.0)
        }.sortedByDescending { it.reachableRangeKm ?: Double.NEGATIVE_INFINITY }

    private fun List<BoschAssistMode>.toAssistModeRangeSummary(): String? =
        mapNotNull { mode ->
            mode.reachableRangeKm
                ?.takeIf { it > 0.0 }
                ?.let { s(R.string.dashboard_assist_range_value, it.toWholeNumber()) }
        }.takeIf { it.isNotEmpty() }
            ?.joinToString(" | ")

    private fun BoschDriveUnit.toPowerOnSummary(): String? = when {
        totalPowerOnHours != null && supportPowerOnHours != null ->
            s(R.string.dashboard_power_on_summary_both, totalPowerOnHours, supportPowerOnHours)
        supportPowerOnHours != null ->
            s(R.string.dashboard_power_on_summary_support_only, supportPowerOnHours)
        totalPowerOnHours != null ->
            s(R.string.dashboard_power_on_summary_total_only, totalPowerOnHours)
        else -> null
    }

    private fun List<BoschActivityDetailPoint>.toTrackPoints(): List<ActivityTrackPointUiModel> {
        val compressed = mutableListOf<ActivityTrackPointUiModel>()
        forEach { point ->
            val latitude = point.latitude?.takeIf { !it.isNaN() } ?: return@forEach
            val longitude = point.longitude?.takeIf { !it.isNaN() } ?: return@forEach
            if (latitude == 0.0 && longitude == 0.0) return@forEach

            val candidate = ActivityTrackPointUiModel(
                latitude = latitude,
                longitude = longitude,
                altitudeMeters = point.altitudeMeters?.takeIf { !it.isNaN() },
                distanceMeters = point.distanceMeters?.takeIf { !it.isNaN() && it >= 0.0 },
            )
            val previous = compressed.lastOrNull()
            if (previous != null && previous.hasSameCoordinates(candidate)) {
                compressed[compressed.lastIndex] = previous.mergeWith(candidate)
            } else {
                compressed += candidate
            }
        }
        return compressed
    }

    private fun List<BoschActivityDetailPoint>.toProfilePoints(): List<ActivityProfilePointUiModel> {
        val compressed = mutableListOf<ActivityProfilePointUiModel>()
        forEach { point ->
            val distanceMeters = point.distanceMeters?.takeIf { !it.isNaN() && it >= 0.0 } ?: return@forEach
            val candidate = ActivityProfilePointUiModel(
                distanceMeters = distanceMeters,
                altitudeMeters = point.altitudeMeters?.takeIf { it >= 0.0 && !it.isNaN() },
                speedKmh = point.speedKmh?.takeIf { it >= 0.0 && !it.isNaN() },
                cadenceRpm = point.cadenceRpm?.takeIf { it >= 0.0 && !it.isNaN() },
                riderPowerWatts = point.riderPowerWatts?.takeIf { it >= 0.0 && !it.isNaN() },
            )
            val previous = compressed.lastOrNull()
            if (previous != null && abs(previous.distanceMeters - candidate.distanceMeters) < DISTANCE_EPSILON_METERS) {
                compressed[compressed.lastIndex] = previous.mergeWith(candidate)
            } else {
                compressed += candidate
            }
        }
        return compressed
    }

    private fun ActivityTrackPointUiModel.hasSameCoordinates(other: ActivityTrackPointUiModel): Boolean =
        abs(latitude - other.latitude) < COORDINATE_EPSILON && abs(longitude - other.longitude) < COORDINATE_EPSILON

    private fun ActivityTrackPointUiModel.mergeWith(other: ActivityTrackPointUiModel): ActivityTrackPointUiModel =
        ActivityTrackPointUiModel(
            latitude = other.latitude,
            longitude = other.longitude,
            altitudeMeters = other.altitudeMeters ?: altitudeMeters,
            distanceMeters = other.distanceMeters ?: distanceMeters,
        )

    private fun ActivityProfilePointUiModel.mergeWith(other: ActivityProfilePointUiModel): ActivityProfilePointUiModel =
        ActivityProfilePointUiModel(
            distanceMeters = other.distanceMeters,
            altitudeMeters = other.altitudeMeters ?: altitudeMeters,
            speedKmh = other.speedKmh ?: speedKmh,
            cadenceRpm = other.cadenceRpm ?: cadenceRpm,
            riderPowerWatts = other.riderPowerWatts ?: riderPowerWatts,
        )

    private fun String.toReadableDateTime(): String =
        runCatching {
            Instant.parse(this)
                .atZone(ZoneId.systemDefault())
                .format(DATE_TIME_FORMATTER)
        }.getOrDefault(this)

    private fun Long.toReadableDateTime(): String =
        Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .format(DATE_TIME_FORMATTER)

    private fun Long.toReadableDateRange(endEpochMillis: Long): String {
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(Locale.getDefault())
        val startDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
        val endDate = Instant.ofEpochMilli(endEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        return if (startDate == endDate) {
            startDate.format(formatter)
        } else {
            "${startDate.format(formatter)} - ${endDate.format(formatter)}"
        }
    }

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

    private fun BoschBatteryHealthBand?.toLabel(): String = when (this) {
        BoschBatteryHealthBand.OPTIMAL -> s(R.string.dashboard_battery_health_optimal)
        BoschBatteryHealthBand.GOOD -> s(R.string.dashboard_battery_health_good)
        BoschBatteryHealthBand.AGED -> s(R.string.dashboard_battery_health_aged)
        BoschBatteryHealthBand.WORN -> s(R.string.dashboard_battery_health_worn)
        BoschBatteryHealthBand.CRITICAL -> s(R.string.dashboard_battery_health_critical)
        null -> s(R.string.pdf_value_not_available)
    }

    private fun BoschBatteryCycleRisk?.toLabel(): String = when (this) {
        BoschBatteryCycleRisk.LOW -> s(R.string.dashboard_battery_cycle_risk_low)
        BoschBatteryCycleRisk.MEDIUM -> s(R.string.dashboard_battery_cycle_risk_medium)
        BoschBatteryCycleRisk.ELEVATED -> s(R.string.dashboard_battery_cycle_risk_elevated)
        null -> s(R.string.pdf_value_not_available)
    }

    private fun BoschBatteryStressLevel?.toLabel(): String = when (this) {
        BoschBatteryStressLevel.GENTLE -> s(R.string.dashboard_battery_stress_gentle)
        BoschBatteryStressLevel.NORMAL -> s(R.string.dashboard_battery_stress_normal)
        BoschBatteryStressLevel.INTENSIVE -> s(R.string.dashboard_battery_stress_intensive)
        null -> s(R.string.pdf_value_not_available)
    }

    private fun BoschBatteryHealthBand?.toIndicatorTone(): DetailSectionIndicatorTone = when (this) {
        BoschBatteryHealthBand.OPTIMAL -> DetailSectionIndicatorTone.POSITIVE
        BoschBatteryHealthBand.GOOD -> DetailSectionIndicatorTone.INFORMATIVE
        BoschBatteryHealthBand.AGED,
        BoschBatteryHealthBand.WORN,
        null,
        -> DetailSectionIndicatorTone.WARNING
        BoschBatteryHealthBand.CRITICAL -> DetailSectionIndicatorTone.DANGER
    }

    private fun s(@StringRes resId: Int, vararg args: Any): String =
        stringResolver.get(resId, args)

    private companion object {
        private val DATE_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        private const val COORDINATE_EPSILON = 0.000001
        private const val DISTANCE_EPSILON_METERS = 0.001
    }
}
