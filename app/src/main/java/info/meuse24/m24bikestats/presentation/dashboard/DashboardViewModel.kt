package info.meuse24.m24bikestats.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.model.BoschAssistMode
import info.meuse24.m24bikestats.domain.model.BoschBattery
import info.meuse24.m24bikestats.domain.model.BoschBike
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
    private val getBikes: GetSmartSystemBikesUseCase,
    private val getBikeDetail: GetSmartSystemBikeDetailUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(isLoading = true))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var cachedActivities: List<BoschActivity> = emptyList()
    private var cachedBikes: List<BoschBike> = emptyList()

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

            val activitiesDeferred = async { getActivities() }
            val bikesDeferred = async { getBikes() }

            val activitiesResult = activitiesDeferred.await()
            val bikesResult = bikesDeferred.await()

            val activities = activitiesResult.getOrElse { error ->
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

            cachedActivities = activities
            cachedBikes = bikes

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    activities = activities.map(::toActivityCardUiModel),
                    bikes = bikes.map(::toBikeCardUiModel),
                    error = null,
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

    fun getActivityDetail(activityId: String): ActivityDetailUiModel? {
        return cachedActivities.firstOrNull { it.id == activityId }?.let(::toActivityDetailUiModel)
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

    private fun toActivityDetailUiModel(activity: BoschActivity): ActivityDetailUiModel {
        return ActivityDetailUiModel(
            title = activity.title,
            metrics = buildList {
                add("Start" to activity.startTime.toReadableDateTime())
                activity.endTime?.let { add("Ende" to it.toReadableDateTime()) }
                add("Dauer" to activity.durationWithoutStopsSeconds.toDurationText())
                add("Distanz" to activity.distanceMeters.toKilometerText())
                activity.startOdometerMeters?.let { add("Start-Kilometerstand" to it.toKilometerText()) }
                activity.bikeId?.let { add("Bike-ID" to it) }
                activity.averageSpeedKmh?.let { add("Ø Geschwindigkeit" to it.toSpeedText()) }
                activity.maxSpeedKmh?.let { add("Max. Geschwindigkeit" to it.toSpeedText()) }
                activity.averageCadenceRpm?.let { add("Ø Kadenz" to "${it.toWholeNumber()} rpm") }
                activity.maxCadenceRpm?.let { add("Max. Kadenz" to "${it.toWholeNumber()} rpm") }
                activity.averageRiderPowerWatts?.let { add("Ø Fahrerleistung" to "${it.toWholeNumber()} W") }
                activity.maxRiderPowerWatts?.let { add("Max. Fahrerleistung" to "${it.toWholeNumber()} W") }
                if (activity.elevationGainMeters != null && activity.elevationLossMeters != null) {
                    add("Höhenmeter" to "+${activity.elevationGainMeters} m / -${activity.elevationLossMeters} m")
                }
                activity.caloriesBurned?.let { add("Kalorien" to "${it.toWholeNumber()} kcal") }
                activity.timeZone?.let { add("Zeitzone" to it) }
            }
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

    private fun Double.toSpeedText(): String =
        String.format(Locale.US, "%.1f km/h", this)

    private fun Double.toWholeNumber(): String =
        String.format(Locale.US, "%.0f", this)

    companion object {
        private val DATE_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    }
}
