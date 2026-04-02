package info.meuse24.m24bikestats.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.model.BoschActivityDetail
import info.meuse24.m24bikestats.domain.model.BoschActivityDetailPoint
import info.meuse24.m24bikestats.domain.model.BoschActivityPage
import info.meuse24.m24bikestats.domain.model.BoschAssistMode
import info.meuse24.m24bikestats.domain.model.BoschBattery
import info.meuse24.m24bikestats.domain.model.BoschBike
import info.meuse24.m24bikestats.domain.usecase.GetSmartSystemActivityDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.GetSmartSystemActivitiesUseCase
import info.meuse24.m24bikestats.domain.usecase.GetSmartSystemBikeDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.GetSmartSystemBikesUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class DashboardViewModel(
    private val getActivities: GetSmartSystemActivitiesUseCase,
    private val getActivityDetail: GetSmartSystemActivityDetailUseCase,
    private val getBikes: GetSmartSystemBikesUseCase,
    private val getBikeDetail: GetSmartSystemBikeDetailUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(isLoading = true))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var cachedActivities: List<BoschActivity> = emptyList()
    private var cachedBikes: List<BoschBike> = emptyList()
    private val cachedActivityDetails = mutableMapOf<String, BoschActivityDetail>()
    private var activityOffset: Int = 0
    private var activityTotalCount: Int = 0

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val hasContent = cachedActivities.isNotEmpty() || cachedBikes.isNotEmpty()
            _uiState.update {
                it.copy(
                    isLoading = !hasContent,
                    isRefreshing = hasContent,
                    error = null,
                )
            }

            val activitiesDeferred = async { getActivities(limit = ACTIVITIES_PAGE_SIZE, offset = 0) }
            val bikesDeferred = async { getBikes() }

            val activitiesResult = activitiesDeferred.await()
            val bikesResult = bikesDeferred.await()

            val activityPage = activitiesResult.getOrElse { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = error.message ?: "Aktivitäten konnten nicht geladen werden",
                    )
                }
                return@launch
            }

            val bikes = bikesResult.getOrElse { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = error.message ?: "Bikes konnten nicht geladen werden",
                    )
                }
                return@launch
            }

            cachedActivities = activityPage.items
            cachedBikes = bikes
            activityOffset = activityPage.offset + activityPage.items.size
            activityTotalCount = activityPage.total

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMoreActivities = false,
                    activityTotalCount = activityPage.total,
                    loadedActivityCount = cachedActivities.size,
                    canLoadMoreActivities = cachedActivities.size < activityPage.total,
                    activities = cachedActivities.map(::toActivityCardUiModel),
                    bikes = bikes.map(::toBikeCardUiModel),
                    error = null,
                )
            }
        }
    }

    fun loadMoreActivities() {
        val state = _uiState.value
        if (state.isLoading || state.isRefreshing || state.isLoadingMoreActivities || !state.canLoadMoreActivities) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreActivities = true, error = null) }

            val nextPage = getActivities(limit = ACTIVITIES_PAGE_SIZE, offset = activityOffset)
                .getOrElse { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingMoreActivities = false,
                            error = error.message ?: "Weitere Aktivitäten konnten nicht geladen werden",
                        )
                    }
                    return@launch
                }

            cachedActivities = cachedActivities + nextPage.items
            activityOffset = nextPage.offset + nextPage.items.size
            activityTotalCount = nextPage.total

            _uiState.update {
                it.copy(
                    isLoadingMoreActivities = false,
                    activityTotalCount = activityTotalCount,
                    loadedActivityCount = cachedActivities.size,
                    canLoadMoreActivities = cachedActivities.size < activityTotalCount,
                    activities = cachedActivities.map(::toActivityCardUiModel),
                )
            }
        }
    }

    fun loadBikeDetail(bikeId: String) {
        if (_uiState.value.selectedBikeId == bikeId && _uiState.value.selectedBikeDetail != null) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedBikeId = bikeId,
                    isBikeDetailLoading = true,
                    error = null,
                )
            }

            val detail = getBikeDetail(bikeId).getOrElse { error ->
                _uiState.update {
                    it.copy(
                        isBikeDetailLoading = false,
                        error = error.message ?: "Bike-Detail konnte nicht geladen werden",
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    selectedBikeId = bikeId,
                    selectedBikeDetail = toBikeDetailUiModel(detail),
                    isBikeDetailLoading = false,
                    error = null,
                )
            }
        }
    }

    fun loadActivityDetail(activityId: String) {
        val activity = cachedActivities.firstOrNull { it.id == activityId }
        if (activity == null) {
            _uiState.update { it.copy(error = "Aktivität nicht gefunden") }
            return
        }

        val cachedDetail = cachedActivityDetails[activityId]
        if (_uiState.value.selectedActivityId == activityId && _uiState.value.selectedActivityDetail != null && cachedDetail != null) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedActivityId = activityId,
                    selectedActivityDetail = if (cachedDetail != null) toActivityDetailUiModel(activity, cachedDetail) else null,
                    isActivityDetailLoading = true,
                    error = null,
                )
            }

            val detail = cachedDetail ?: getActivityDetail(activityId).getOrElse { error ->
                _uiState.update {
                    it.copy(
                        isActivityDetailLoading = false,
                        error = error.message ?: "Aktivitätsdetails konnten nicht geladen werden",
                    )
                }
                return@launch
            }

            cachedActivityDetails[activityId] = detail

            _uiState.update {
                it.copy(
                    selectedActivityId = activityId,
                    selectedActivityDetail = toActivityDetailUiModel(activity, detail),
                    isActivityDetailLoading = false,
                    error = null,
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun toActivityCardUiModel(activity: BoschActivity): ActivityCardUiModel {
        return ActivityCardUiModel(
            id = activity.id,
            title = activity.title,
            startedAt = activity.startTime,
            dateLabel = activity.startTime.toReadableDateTime(),
            distanceLabel = activity.distanceMeters.toKilometerText(),
            durationLabel = activity.durationWithoutStopsSeconds.toDurationText(),
            speedLabel = listOfNotNull(
                activity.averageSpeedKmh?.let { "Ø ${it.toSpeedText()}" },
                activity.maxSpeedKmh?.let { "max ${it.toSpeedText()}" },
            ).joinToString(" • ").ifBlank { "Keine Geschwindigkeitsdaten" },
            powerLabel = activity.averageRiderPowerWatts?.let { average ->
                val max = activity.maxRiderPowerWatts?.let { ", max ${it.toWholeNumber()} W" }.orEmpty()
                "Ø ${average.toWholeNumber()} W$max"
            },
            elevationLabel = if (activity.elevationGainMeters != null && activity.elevationLossMeters != null) {
                "+${activity.elevationGainMeters} m / -${activity.elevationLossMeters} m"
            } else null,
            caloriesLabel = activity.caloriesBurned?.let { "${it.toWholeNumber()} kcal" },
        )
    }

    private fun toActivityDetailUiModel(
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
        val directionsUrl = buildDirectionsUrl(startCoordinate, endCoordinate)
        val shareText = buildShareText(activity, startCoordinate, endCoordinate, directionsUrl)

        return ActivityDetailUiModel(
            title = activity.title,
            subtitle = "${detail.points.size} Detailpunkte • ${geoPoints.size} mit GPS",
            summary = buildList {
                add("Start" to activity.startTime.toReadableDateTime())
                add("Distanz" to activity.distanceMeters.toKilometerText())
                add("Dauer" to activity.durationWithoutStopsSeconds.toDurationText())
                activity.averageSpeedKmh?.let { add("Ø Geschwindigkeit" to it.toSpeedText()) }
            },
            sections = buildList {
                add(
                    DetailSectionUiModel(
                        title = "Zeit & Strecke",
                        rows = buildList {
                            add("Start" to activity.startTime.toReadableDateTime())
                            activity.endTime?.let { add("Ende" to it.toReadableDateTime()) }
                            add("Dauer ohne Stopps" to activity.durationWithoutStopsSeconds.toDurationText())
                            add("Distanz" to activity.distanceMeters.toKilometerText())
                            lastDistanceMeters?.let { add("Track-Distanz" to it.toKilometerText()) }
                            activity.startOdometerMeters?.let { add("Start-Kilometerstand" to it.toKilometerText()) }
                            activity.timeZone?.let { add("Zeitzone" to it) }
                            activity.bikeId?.let { add("Bike-ID" to it) }
                        }
                    )
                )

                add(
                    DetailSectionUiModel(
                        title = "Leistung & Fahrt",
                        rows = buildList {
                            activity.averageSpeedKmh?.let { add("Ø Geschwindigkeit" to it.toSpeedText()) }
                            activity.maxSpeedKmh?.let { add("Max. Geschwindigkeit" to it.toSpeedText()) }
                            if (speedPoints.isNotEmpty()) {
                                add("Track-Speed max." to speedPoints.max().toSpeedText())
                            }
                            activity.averageCadenceRpm?.let { add("Ø Kadenz" to "${it.toWholeNumber()} rpm") }
                            activity.maxCadenceRpm?.let { add("Max. Kadenz" to "${it.toWholeNumber()} rpm") }
                            if (cadencePoints.isNotEmpty()) {
                                add("Track-Kadenz Ø" to "${cadencePoints.average().toWholeNumber()} rpm")
                            }
                            activity.averageRiderPowerWatts?.let { add("Ø Fahrerleistung" to "${it.toWholeNumber()} W") }
                            activity.maxRiderPowerWatts?.let { add("Max. Fahrerleistung" to "${it.toWholeNumber()} W") }
                            if (riderPowerPoints.isNotEmpty()) {
                                add("Track-Leistung Ø" to "${riderPowerPoints.average().toWholeNumber()} W")
                            }
                        }
                    )
                )

                add(
                    DetailSectionUiModel(
                        title = "Höhenmeter & Energie",
                        rows = buildList {
                            if (activity.elevationGainMeters != null && activity.elevationLossMeters != null) {
                                add("Höhenmeter" to "+${activity.elevationGainMeters} m / -${activity.elevationLossMeters} m")
                            }
                            if (altitudePoints.isNotEmpty()) {
                                add(
                                    "Höhenprofil" to "${altitudePoints.minOrNull()?.toWholeNumber()} m bis ${altitudePoints.maxOrNull()?.toWholeNumber()} m"
                                )
                            }
                            activity.caloriesBurned?.let { add("Kalorien" to "${it.toWholeNumber()} kcal") }
                        }
                    )
                )

                add(
                    DetailSectionUiModel(
                        title = "Track & GPS",
                        rows = buildList {
                            add("Detailpunkte" to detail.points.size.toString())
                            add("GPS-Punkte" to geoPoints.size.toString())
                            startCoordinate?.let {
                                add("Startkoordinate" to "${it.latitude!!.toCoordinateText()}, ${it.longitude!!.toCoordinateText()}")
                            }
                            endCoordinate?.let {
                                add("Zielkoordinate" to "${it.latitude!!.toCoordinateText()}, ${it.longitude!!.toCoordinateText()}")
                            }
                        },
                        actions = buildList {
                            add(
                                DetailSectionActionUiModel(
                                    label = "Teilen",
                                    type = DetailSectionActionType.SHARE,
                                    payload = shareText,
                                )
                            )
                            directionsUrl?.let {
                                add(
                                    DetailSectionActionUiModel(
                                        label = "Kartenanzeige",
                                        type = DetailSectionActionType.MAP,
                                        payload = it,
                                    )
                                )
                            }
                        },
                    )
                )
            }.filter { it.rows.isNotEmpty() }
        )
    }

    private fun toBikeCardUiModel(bike: BoschBike): BikeCardUiModel {
        return BikeCardUiModel(
            id = bike.id,
            title = bike.driveUnit?.productName ?: "Bike",
            subtitle = bike.headUnit?.productName,
            odometerLabel = bike.driveUnit?.odometerMeters
                ?.div(1000.0)
                ?.let { String.format(Locale.US, "%.1f km", it) },
            assistSpeedLabel = bike.driveUnit?.maximumAssistanceSpeedKmh?.toSpeedText(),
            batterySummary = bike.batteries.firstOrNull()?.let { battery ->
                buildString {
                    append(battery.productName ?: "Batterie")
                    battery.totalChargeCycles?.let { append(" • ${String.format(Locale.US, "%.1f", it)} Zyklen") }
                }
            }
        )
    }

    private fun toBikeDetailUiModel(bike: BoschBike): BikeDetailUiModel {
        return BikeDetailUiModel(
            title = bike.driveUnit?.productName ?: "Bike",
            subtitle = bike.headUnit?.productName,
            sections = buildList {
                add(
                    DetailSectionUiModel(
                        title = "Übersicht",
                        rows = buildList {
                            add("Bike-ID" to bike.id)
                            bike.createdAt?.let { add("Angelegt" to it.toReadableDateTime()) }
                            bike.language?.let { add("Sprache" to it) }
                            bike.driveUnit?.odometerMeters?.div(1000.0)
                                ?.let { add("Kilometerstand" to String.format(Locale.US, "%.1f km", it)) }
                            bike.driveUnit?.maximumAssistanceSpeedKmh
                                ?.let { add("Max. Unterstützung" to it.toSpeedText()) }
                            bike.driveUnit?.rearWheelCircumferenceMillimeters
                                ?.let { add("Radumfang" to "${it.toWholeNumber()} mm") }
                        }
                    )
                )

                bike.driveUnit?.let { driveUnit ->
                    add(
                        DetailSectionUiModel(
                            title = "Drive Unit",
                            rows = buildList {
                                driveUnit.productName?.let { add("Produkt" to it) }
                                driveUnit.partNumber?.let { add("Teilenummer" to it) }
                                driveUnit.serialNumber?.let { add("Seriennummer" to it) }
                                driveUnit.walkAssistEnabled?.let { add("Walk Assist" to if (it) "aktiv" else "inaktiv") }
                                driveUnit.walkAssistMaximumSpeedKmh
                                    ?.let { add("Walk Assist max." to it.toSpeedText()) }
                                driveUnit.totalPowerOnHours?.let { add("Einschaltzeit gesamt" to "$it h") }
                                driveUnit.supportPowerOnHours?.let { add("Mit Motorunterstützung" to "$it h") }
                                if (driveUnit.activeAssistModes.isNotEmpty()) {
                                    add("Assist Modes" to driveUnit.activeAssistModes.toAssistModeSummary())
                                }
                            }
                        )
                    )
                }

                if (bike.batteries.isNotEmpty()) {
                    add(
                        DetailSectionUiModel(
                            title = "Batterien",
                            rows = bike.batteries.flatMapIndexed { index, battery ->
                                battery.toRows(prefix = "Batterie ${index + 1}")
                            }
                        )
                    )
                }

                bike.remoteControl?.let { remote ->
                    add(
                        DetailSectionUiModel(
                            title = "Remote",
                            rows = remote.toRows()
                        )
                    )
                }

                bike.headUnit?.let { headUnit ->
                    add(
                        DetailSectionUiModel(
                            title = "Head Unit",
                            rows = headUnit.toRows()
                        )
                    )
                }
            }
        )
    }

    private fun BoschBattery.toRows(prefix: String): List<Pair<String, String>> = buildList {
        productName?.let { add("$prefix Produkt" to it) }
        partNumber?.let { add("$prefix Teilenummer" to it) }
        serialNumber?.let { add("$prefix Seriennummer" to it) }
        deliveredWhOverLifetime?.let { add("$prefix Gelieferte Energie" to "$it Wh") }
        totalChargeCycles?.let { add("$prefix Ladezyklen gesamt" to String.format(Locale.US, "%.1f", it)) }
        onBikeChargeCycles?.let { add("$prefix On-Bike-Zyklen" to String.format(Locale.US, "%.1f", it)) }
        offBikeChargeCycles?.let { add("$prefix Off-Bike-Zyklen" to String.format(Locale.US, "%.1f", it)) }
    }

    private fun info.meuse24.m24bikestats.domain.model.BoschComponent.toRows(): List<Pair<String, String>> = buildList {
        productName?.let { add("Produkt" to it) }
        partNumber?.let { add("Teilenummer" to it) }
        serialNumber?.let { add("Seriennummer" to it) }
    }

    private fun List<BoschAssistMode>.toAssistModeSummary(): String =
        joinToString(" | ") { mode ->
            mode.reachableRangeKm?.let { "${mode.name} (${it.toWholeNumber()} km)" } ?: mode.name
        }

    private fun String.toReadableDateTime(): String {
        return runCatching {
            Instant.parse(this)
                .atZone(ZoneId.systemDefault())
                .format(DATE_TIME_FORMATTER)
        }.getOrDefault(this)
    }

    private fun Int.toDurationText(): String {
        val hours = this / 3600
        val minutes = (this % 3600) / 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes} min"
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

    private fun buildDirectionsUrl(
        startCoordinate: BoschActivityDetailPoint?,
        endCoordinate: BoschActivityDetailPoint?,
    ): String? {
        val startLat = startCoordinate?.latitude ?: return null
        val startLon = startCoordinate.longitude ?: return null
        val endLat = endCoordinate?.latitude ?: return "geo:0,0?q=${startLat.toCoordinateText()},${startLon.toCoordinateText()}"
        val endLon = endCoordinate.longitude ?: return "geo:0,0?q=${startLat.toCoordinateText()},${startLon.toCoordinateText()}"

        return if (startLat == endLat && startLon == endLon) {
            "geo:0,0?q=${startLat.toCoordinateText()},${startLon.toCoordinateText()}"
        } else {
            "https://www.google.com/maps/dir/?api=1&origin=${startLat.toCoordinateText()},${startLon.toCoordinateText()}&destination=${endLat.toCoordinateText()},${endLon.toCoordinateText()}&travelmode=bicycling"
        }
    }

    private fun buildShareText(
        activity: BoschActivity,
        startCoordinate: BoschActivityDetailPoint?,
        endCoordinate: BoschActivityDetailPoint?,
        directionsUrl: String?,
    ): String {
        val startText = startCoordinate?.let {
            "${it.latitude?.toCoordinateText()}, ${it.longitude?.toCoordinateText()}"
        }
        val endText = endCoordinate?.let {
            "${it.latitude?.toCoordinateText()}, ${it.longitude?.toCoordinateText()}"
        }

        return buildString {
            appendLine(activity.title)
            appendLine("Start: ${activity.startTime.toReadableDateTime()}")
            appendLine("Distanz: ${activity.distanceMeters.toKilometerText()}")
            activity.averageSpeedKmh?.let { appendLine("Ø Geschwindigkeit: ${it.toSpeedText()}") }
            startText?.let { appendLine("Startkoordinate: $it") }
            endText?.let { appendLine("Zielkoordinate: $it") }
            directionsUrl?.let { appendLine("Karte: $it") }
        }.trim()
    }

    companion object {
        private const val ACTIVITIES_PAGE_SIZE = 20
        private val DATE_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    }
}
