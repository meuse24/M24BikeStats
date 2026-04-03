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
import info.meuse24.m24bikestats.domain.usecase.ExportSmartSystemActivitiesCsvUseCase
import info.meuse24.m24bikestats.domain.usecase.GetCachedSmartSystemActivityUseCase
import info.meuse24.m24bikestats.domain.usecase.GetCachedSmartSystemActivityDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.GetCachedSmartSystemBikeUseCase
import info.meuse24.m24bikestats.domain.usecase.GetSmartSystemActivitiesUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemActivityDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemActivitiesUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemBikeDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemBikesUseCase
import info.meuse24.m24bikestats.domain.usecase.RefreshSmartSystemActivitiesUseCase
import info.meuse24.m24bikestats.domain.usecase.RefreshSmartSystemActivityDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.RefreshSmartSystemBikeDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.RefreshSmartSystemBikesUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
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
    private val observeCachedActivities: ObserveCachedSmartSystemActivitiesUseCase,
    private val observeCachedActivityDetail: ObserveCachedSmartSystemActivityDetailUseCase,
    private val observeCachedBikes: ObserveCachedSmartSystemBikesUseCase,
    private val observeCachedBikeDetail: ObserveCachedSmartSystemBikeDetailUseCase,
    private val getCachedActivity: GetCachedSmartSystemActivityUseCase,
    private val getCachedActivityDetail: GetCachedSmartSystemActivityDetailUseCase,
    private val getCachedBike: GetCachedSmartSystemBikeUseCase,
    private val getActivities: GetSmartSystemActivitiesUseCase,
    private val refreshActivitiesUseCase: RefreshSmartSystemActivitiesUseCase,
    private val exportActivitiesCsv: ExportSmartSystemActivitiesCsvUseCase,
    private val refreshActivityDetailUseCase: RefreshSmartSystemActivityDetailUseCase,
    private val refreshBikesUseCase: RefreshSmartSystemBikesUseCase,
    private val refreshBikeDetailUseCase: RefreshSmartSystemBikeDetailUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(isLoading = true))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var cachedActivities: List<BoschActivity> = emptyList()
    private var activityOffset: Int = 0
    private var activityTotalCount: Int = 0
    private var activityDetailObservationJob: Job? = null
    private var bikeDetailObservationJob: Job? = null

    init {
        observeActivities()
        observeBikes()
        refresh(force = false)
    }

    private fun observeActivities() {
        viewModelScope.launch {
            observeCachedActivities().collectLatest { activities ->
                cachedActivities = activities
                _uiState.update { current ->
                    val resolvedTotal = when {
                        current.activityTotalCount > 0 -> maxOf(current.activityTotalCount, activities.size)
                        activityTotalCount > 0 -> maxOf(activityTotalCount, activities.size)
                        else -> activities.size
                    }
                    activityOffset = activities.size
                    val hasInitialContent = activities.isNotEmpty() || current.bikes.isNotEmpty()
                    current.copy(
                        isLoading = current.isLoading && !hasInitialContent,
                        activities = activities.map(::toActivityCardUiModel),
                        loadedActivityCount = activities.size,
                        activityTotalCount = resolvedTotal,
                        canLoadMoreActivities = activities.size < resolvedTotal,
                    )
                }
            }
        }
    }

    private fun observeBikes() {
        viewModelScope.launch {
            observeCachedBikes().collectLatest { bikes ->
                _uiState.update { current ->
                    val hasInitialContent = current.activities.isNotEmpty() || bikes.isNotEmpty()
                    current.copy(
                        isLoading = current.isLoading && !hasInitialContent,
                        bikes = bikes.map(::toBikeCardUiModel),
                    )
                }
            }
        }
    }

    fun refresh(force: Boolean = true) {
        viewModelScope.launch {
            val hasContent = _uiState.value.activities.isNotEmpty() || _uiState.value.bikes.isNotEmpty()
            _uiState.update {
                it.copy(
                    isLoading = !hasContent,
                    isRefreshing = hasContent,
                    error = null,
                )
            }

            val activitiesDeferred = async {
                refreshActivitiesUseCase(limit = ACTIVITIES_PAGE_SIZE, offset = 0, force = force)
            }
            val bikesDeferred = async { refreshBikesUseCase(force = force) }

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

            bikesResult.getOrElse { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = error.message ?: "Bikes konnten nicht geladen werden",
                    )
                }
                return@launch
            }

            if (activityPage != null) {
                activityOffset = activityPage.offset + activityPage.items.size
                activityTotalCount = activityPage.total
            } else {
                activityOffset = cachedActivities.size
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMoreActivities = false,
                    activityTotalCount = activityPage?.total ?: activityTotalCount,
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

            activityOffset = nextPage.offset + nextPage.items.size
            activityTotalCount = nextPage.total

            _uiState.update {
                it.copy(
                    isLoadingMoreActivities = false,
                    activityTotalCount = activityTotalCount,
                )
            }
        }
    }

    fun exportAllActivitiesCsv() {
        val state = _uiState.value
        if (state.isLoading || state.isRefreshing || state.isExportingActivitiesCsv) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExportingActivitiesCsv = true,
                    exportLoadedActivityCount = 0,
                    exportTotalActivityCount = 0,
                    pendingActivitiesCsvExport = null,
                    error = null,
                )
            }

            val export = exportActivitiesCsv { loadedCount, totalCount ->
                _uiState.update {
                    it.copy(
                        exportLoadedActivityCount = loadedCount,
                        exportTotalActivityCount = totalCount,
                    )
                }
            }.getOrElse { error ->
                _uiState.update {
                    it.copy(
                        isExportingActivitiesCsv = false,
                        exportLoadedActivityCount = 0,
                        exportTotalActivityCount = 0,
                        error = error.message ?: "CSV-Export konnte nicht erstellt werden",
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isExportingActivitiesCsv = false,
                    exportLoadedActivityCount = export.activityCount,
                    exportTotalActivityCount = export.activityCount,
                    pendingActivitiesCsvExport = ActivitiesCsvExportUiModel(
                        fileName = export.fileName,
                        csvContent = export.csvContent,
                        activityCount = export.activityCount,
                    ),
                    error = null,
                )
            }
        }
    }

    fun onActivitiesCsvExportHandled() {
        _uiState.update {
            it.copy(
                pendingActivitiesCsvExport = null,
                exportLoadedActivityCount = 0,
                exportTotalActivityCount = 0,
            )
        }
    }

    fun loadBikeDetail(bikeId: String) {
        bikeDetailObservationJob?.cancel()
        viewModelScope.launch {
            val cachedBike = getCachedBike(bikeId)
            _uiState.update {
                it.copy(
                    selectedBikeId = bikeId,
                    selectedBikeDetail = cachedBike?.let(::toBikeDetailUiModel),
                    isBikeDetailLoading = cachedBike == null,
                    isBikeDetailRefreshing = cachedBike != null,
                    error = null,
                )
            }

            bikeDetailObservationJob = viewModelScope.launch {
                observeCachedBikeDetail(bikeId).collectLatest { bike ->
                    if (_uiState.value.selectedBikeId != bikeId) return@collectLatest
                    _uiState.update {
                        it.copy(
                            selectedBikeDetail = bike?.let(::toBikeDetailUiModel),
                            isBikeDetailLoading = bike == null && it.isBikeDetailLoading,
                        )
                    }
                }
            }

            val result = refreshBikeDetailUseCase(bikeId, force = false).getOrElse { error ->
                _uiState.update {
                    it.copy(
                        isBikeDetailLoading = false,
                        isBikeDetailRefreshing = false,
                        selectedBikeDetail = cachedBike?.let(::toBikeDetailUiModel),
                        error = error.message ?: "Bike-Detail konnte nicht geladen werden",
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isBikeDetailLoading = false,
                    isBikeDetailRefreshing = false,
                    error = null,
                )
            }
        }
    }

    fun loadActivityDetail(activityId: String) {
        activityDetailObservationJob?.cancel()
        viewModelScope.launch {
            val activity = getCachedActivity(activityId)
            if (activity == null) {
                _uiState.update { it.copy(error = "Aktivität nicht gefunden") }
                return@launch
            }

            val cachedDetail = getCachedActivityDetail(activityId)

            _uiState.update {
                it.copy(
                    selectedActivityId = activityId,
                    selectedActivityDetail = if (cachedDetail != null) toActivityDetailUiModel(activity, cachedDetail) else null,
                    isActivityDetailLoading = cachedDetail == null,
                    isActivityDetailRefreshing = cachedDetail != null,
                    error = null,
                )
            }

            activityDetailObservationJob = viewModelScope.launch {
                observeCachedActivityDetail(activityId).collectLatest { detail ->
                    if (_uiState.value.selectedActivityId != activityId) return@collectLatest
                    _uiState.update {
                        it.copy(
                            selectedActivityDetail = detail?.let { cached -> toActivityDetailUiModel(activity, cached) },
                            isActivityDetailLoading = detail == null && it.isActivityDetailLoading,
                        )
                    }
                }
            }

            refreshActivityDetailUseCase(activityId, force = false).getOrElse { error ->
                _uiState.update {
                    it.copy(
                        isActivityDetailLoading = false,
                        isActivityDetailRefreshing = false,
                        selectedActivityDetail = cachedDetail?.let { toActivityDetailUiModel(activity, it) },
                        error = error.message ?: "Aktivitätsdetails konnten nicht geladen werden",
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isActivityDetailLoading = false,
                    isActivityDetailRefreshing = false,
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
                                )
                            )
                            if (trackPoints.isNotEmpty()) {
                                add(
                                    DetailSectionActionUiModel(
                                        label = "Kartenanzeige",
                                        type = DetailSectionActionType.MAP,
                                    )
                                )
                            }
                        },
                    )
                )
            }.filter { it.rows.isNotEmpty() },
            trackPoints = trackPoints,
            profilePoints = profilePoints,
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

    companion object {
        private const val ACTIVITIES_PAGE_SIZE = 20
        private val DATE_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    }
}
