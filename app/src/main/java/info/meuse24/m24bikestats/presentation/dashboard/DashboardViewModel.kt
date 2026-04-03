package info.meuse24.m24bikestats.presentation.dashboard

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.meuse24.m24bikestats.R
import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.model.BoschActivityDetail
import info.meuse24.m24bikestats.domain.model.BoschActivityDetailPoint
import info.meuse24.m24bikestats.domain.model.BoschActivityPage
import info.meuse24.m24bikestats.domain.model.BoschAssistMode
import info.meuse24.m24bikestats.domain.model.BoschBattery
import info.meuse24.m24bikestats.domain.model.BoschBike
import info.meuse24.m24bikestats.domain.model.CsvSeparator
import info.meuse24.m24bikestats.domain.usecase.ExportSmartSystemActivityDetailsCsvUseCase
import info.meuse24.m24bikestats.domain.usecase.ExportSmartSystemActivitiesCsvUseCase
import info.meuse24.m24bikestats.domain.usecase.GetCachedSmartSystemActivityUseCase
import info.meuse24.m24bikestats.domain.usecase.GetCachedSmartSystemActivityDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.GetCachedSmartSystemBikeUseCase
import info.meuse24.m24bikestats.domain.usecase.GetSmartSystemActivitiesUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemActivityDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemActivitiesUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemBikeDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemBikesUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveAppSettingsUseCase
import info.meuse24.m24bikestats.domain.usecase.RefreshSmartSystemActivitiesUseCase
import info.meuse24.m24bikestats.domain.usecase.RefreshSmartSystemActivityDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.RefreshSmartSystemBikeDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.RefreshSmartSystemBikesUseCase
import info.meuse24.m24bikestats.domain.usecase.SyncSmartSystemCloudUseCase
import info.meuse24.m24bikestats.domain.usecase.UpdateCsvSeparatorUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
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
    private val exportActivityDetailsCsv: ExportSmartSystemActivityDetailsCsvUseCase,
    private val refreshActivityDetailUseCase: RefreshSmartSystemActivityDetailUseCase,
    private val refreshBikesUseCase: RefreshSmartSystemBikesUseCase,
    private val refreshBikeDetailUseCase: RefreshSmartSystemBikeDetailUseCase,
    private val syncSmartSystemCloudUseCase: SyncSmartSystemCloudUseCase,
    private val observeAppSettings: ObserveAppSettingsUseCase,
    private val updateCsvSeparatorUseCase: UpdateCsvSeparatorUseCase,
    private val stringResolver: DashboardStringResolver,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(isInitialLoading = true))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var activityOffset: Int = 0
    private var activityTotalCount: Int = 0
    private var activityDetailObservationJob: Job? = null
    private var bikeDetailObservationJob: Job? = null

    init {
        observeActivities()
        observeBikes()
        observeSettings()
        refresh(force = false)
    }

    private fun observeActivities() {
        viewModelScope.launch {
            observeCachedActivities().collectLatest { activities ->
                val allActivities = activities.map(::toActivityCardUiModel)
                val presentedActivities = filterPresentedActivities(
                    activities = allActivities,
                    searchQuery = _uiState.value.activitySearchQuery,
                    dateRangeFilter = _uiState.value.activityDateRangeFilter,
                    sortOption = _uiState.value.activitySortOption,
                )
                _uiState.update { current ->
                    val resolvedTotal = when {
                        current.activityTotalCount > 0 -> maxOf(current.activityTotalCount, activities.size)
                        activityTotalCount > 0 -> maxOf(activityTotalCount, activities.size)
                        else -> activities.size
                    }
                    activityOffset = activities.size
                    val hasInitialContent = activities.isNotEmpty() || current.bikes.isNotEmpty()
                    current.copy(
                        isInitialLoading = current.isInitialLoading && !hasInitialContent,
                        allActivities = allActivities,
                        activities = presentedActivities,
                        loadedActivityCount = activities.size,
                        visibleActivityCount = presentedActivities.size,
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
                    val hasInitialContent = current.allActivities.isNotEmpty() || bikes.isNotEmpty()
                    current.copy(
                        isInitialLoading = current.isInitialLoading && !hasInitialContent,
                        bikes = bikes.map(::toBikeCardUiModel),
                    )
                }
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            observeAppSettings().collectLatest { settings ->
                _uiState.update { current ->
                    current.copy(csvSeparator = settings.csvSeparator)
                }
            }
        }
    }

    fun refresh(force: Boolean = true) {
        viewModelScope.launch {
            val hasContent = _uiState.value.activities.isNotEmpty() || _uiState.value.bikes.isNotEmpty()
            _uiState.update {
                it.copy(
                    isInitialLoading = !hasContent,
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
                        isInitialLoading = false,
                        isRefreshing = false,
                        error = error.message ?: s(R.string.dashboard_error_activities_load),
                    )
                }
                return@launch
            }

            bikesResult.getOrElse { error ->
                _uiState.update {
                    it.copy(
                        isInitialLoading = false,
                        isRefreshing = false,
                        error = error.message ?: s(R.string.dashboard_error_bikes_load),
                    )
                }
                return@launch
            }

            if (activityPage != null) {
                activityOffset = activityPage.offset + activityPage.items.size
                activityTotalCount = activityPage.total
            } else {
                activityOffset = _uiState.value.loadedActivityCount
            }

            _uiState.update {
                it.copy(
                    isInitialLoading = false,
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
        if (state.isInitialLoading || state.isRefreshing || state.isLoadingMoreActivities || !state.canLoadMoreActivities) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreActivities = true, error = null) }

            val nextPage = getActivities(limit = ACTIVITIES_PAGE_SIZE, offset = activityOffset)
                .getOrElse { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingMoreActivities = false,
                            error = error.message ?: s(R.string.dashboard_error_more_activities_load),
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
        if (
            state.isInitialLoading ||
            state.isRefreshing ||
            state.isSyncingCloudData ||
            state.isExportingActivitiesCsv ||
            state.isExportingActivityDetailsCsv
        ) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExportingActivitiesCsv = true,
                    exportLoadedActivityCount = 0,
                    exportTotalActivityCount = 0,
                    pendingActivitiesCsvExport = null,
                    pendingActivityDetailsCsvExport = null,
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
                        error = error.message ?: s(R.string.dashboard_error_csv_export),
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
                    lastActivitiesCsvExport = ActivitiesCsvExportSummaryUiModel(
                        fileName = export.fileName,
                        activityCount = export.activityCount,
                        exportedAtLabel = LocalDateTime.now().format(EXPORT_DATE_TIME_FORMATTER),
                    ),
                    error = null,
                )
            }
        }
    }

    fun exportVisibleActivityDetailsCsv() {
        val state = _uiState.value
        if (
            state.isInitialLoading ||
            state.isRefreshing ||
            state.isSyncingCloudData ||
            state.isExportingActivitiesCsv ||
            state.isExportingActivityDetailsCsv
        ) return

        val activityIds = state.activities.map { it.id }.distinct()
        if (activityIds.isEmpty()) {
            _uiState.update { it.copy(error = s(R.string.dashboard_error_no_visible_activities)) }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExportingActivityDetailsCsv = true,
                    exportDetailedLoadedActivityCount = 0,
                    exportDetailedTotalActivityCount = activityIds.size,
                    pendingActivityDetailsCsvExport = null,
                    error = null,
                )
            }

            val export = exportActivityDetailsCsv(activityIds) { processedCount, totalCount ->
                _uiState.update {
                    it.copy(
                        exportDetailedLoadedActivityCount = processedCount,
                        exportDetailedTotalActivityCount = totalCount,
                    )
                }
            }.getOrElse { error ->
                _uiState.update {
                    it.copy(
                        isExportingActivityDetailsCsv = false,
                        exportDetailedLoadedActivityCount = 0,
                        exportDetailedTotalActivityCount = 0,
                        error = error.message ?: s(R.string.dashboard_error_detail_csv_export),
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isExportingActivityDetailsCsv = false,
                    exportDetailedLoadedActivityCount = export.activityCount,
                    exportDetailedTotalActivityCount = export.activityCount,
                    pendingActivityDetailsCsvExport = ActivityDetailsCsvExportUiModel(
                        fileName = export.fileName,
                        csvContent = export.csvContent,
                        activityCount = export.activityCount,
                        detailPointCount = export.detailPointCount,
                    ),
                    lastActivityDetailsCsvExport = ActivityDetailsCsvExportSummaryUiModel(
                        fileName = export.fileName,
                        activityCount = export.activityCount,
                        detailPointCount = export.detailPointCount,
                        exportedAtLabel = LocalDateTime.now().format(EXPORT_DATE_TIME_FORMATTER),
                    ),
                    error = null,
                )
            }
        }
    }

    fun updateActivityDateRangeFilter(filter: ActivityDateRangeFilter) {
        _uiState.update { current ->
            val presentedActivities = filterPresentedActivities(
                activities = current.allActivities,
                searchQuery = current.activitySearchQuery,
                dateRangeFilter = filter,
                sortOption = current.activitySortOption,
            )
            current.copy(
                activityDateRangeFilter = filter,
                activities = presentedActivities,
                visibleActivityCount = presentedActivities.size,
            )
        }
    }

    fun updateActivitySortOption(sortOption: ActivitySortOption) {
        _uiState.update { current ->
            val presentedActivities = filterPresentedActivities(
                activities = current.allActivities,
                searchQuery = current.activitySearchQuery,
                dateRangeFilter = current.activityDateRangeFilter,
                sortOption = sortOption,
            )
            current.copy(
                activitySortOption = sortOption,
                activities = presentedActivities,
                visibleActivityCount = presentedActivities.size,
            )
        }
    }

    fun updateActivitySearchQuery(searchQuery: String) {
        _uiState.update { current ->
            val presentedActivities = filterPresentedActivities(
                activities = current.allActivities,
                searchQuery = searchQuery,
                dateRangeFilter = current.activityDateRangeFilter,
                sortOption = current.activitySortOption,
            )
            current.copy(
                activitySearchQuery = searchQuery,
                activities = presentedActivities,
                visibleActivityCount = presentedActivities.size,
            )
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

    fun onActivityDetailsCsvExportHandled() {
        _uiState.update {
            it.copy(
                pendingActivityDetailsCsvExport = null,
                exportDetailedLoadedActivityCount = 0,
                exportDetailedTotalActivityCount = 0,
            )
        }
    }

    fun updateCsvSeparator(separator: CsvSeparator) {
        if (_uiState.value.csvSeparator == separator) return
        viewModelScope.launch {
            updateCsvSeparatorUseCase(separator)
        }
    }

    fun syncCloudData() {
        val state = _uiState.value
        if (
            state.isInitialLoading ||
            state.isRefreshing ||
            state.isSyncingCloudData ||
            state.isExportingActivitiesCsv ||
            state.isExportingActivityDetailsCsv
        ) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSyncingCloudData = true,
                    syncLoadedActivityCount = 0,
                    syncTotalActivityCount = 0,
                    error = null,
                )
            }

            val summary = syncSmartSystemCloudUseCase { loadedCount, totalCount ->
                _uiState.update {
                    it.copy(
                        syncLoadedActivityCount = loadedCount,
                        syncTotalActivityCount = totalCount,
                    )
                }
            }.getOrElse { error ->
                _uiState.update {
                    it.copy(
                        isSyncingCloudData = false,
                        syncLoadedActivityCount = 0,
                        syncTotalActivityCount = 0,
                        error = error.message ?: s(R.string.dashboard_error_cloud_sync),
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isSyncingCloudData = false,
                    syncLoadedActivityCount = summary.activityCount,
                    syncTotalActivityCount = summary.activityCount,
                    lastCloudSyncSummary = CloudSyncSummaryUiModel(
                        activityCount = summary.activityCount,
                        bikeCount = summary.bikeCount,
                        syncedAtLabel = LocalDateTime.now().format(EXPORT_DATE_TIME_FORMATTER),
                    ),
                    error = null,
                )
            }
        }
    }

    fun loadBikeDetail(bikeId: String) {
        loadBikeDetailInternal(bikeId = bikeId, force = false)
    }

    fun refreshBikeDetail(bikeId: String) {
        loadBikeDetailInternal(bikeId = bikeId, force = true)
    }

    private fun loadBikeDetailInternal(
        bikeId: String,
        force: Boolean,
    ) {
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

            refreshBikeDetailUseCase(bikeId, force = force).getOrElse { error ->
                _uiState.update {
                    it.copy(
                        isBikeDetailLoading = false,
                        isBikeDetailRefreshing = false,
                        selectedBikeDetail = cachedBike?.let(::toBikeDetailUiModel),
                        error = error.message ?: s(R.string.dashboard_error_bike_detail_load),
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
        loadActivityDetailInternal(activityId = activityId, force = false)
    }

    fun refreshActivityDetail(activityId: String) {
        loadActivityDetailInternal(activityId = activityId, force = true)
    }

    private fun loadActivityDetailInternal(
        activityId: String,
        force: Boolean,
    ) {
        activityDetailObservationJob?.cancel()
        viewModelScope.launch {
            val activity = getCachedActivity(activityId)
            if (activity == null) {
                _uiState.update { it.copy(error = s(R.string.dashboard_error_activity_not_found)) }
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

            refreshActivityDetailUseCase(activityId, force = force).getOrElse { error ->
                _uiState.update {
                    it.copy(
                        isActivityDetailLoading = false,
                        isActivityDetailRefreshing = false,
                        selectedActivityDetail = cachedDetail?.let { toActivityDetailUiModel(activity, it) },
                        error = error.message ?: s(R.string.dashboard_error_activity_detail_load),
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
            } else null,
            caloriesLabel = activity.caloriesBurned?.let { s(R.string.dashboard_calories_value, it.toWholeNumber()) },
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
                        }
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
                        }
                    )
                )

                add(
                    DetailSectionUiModel(
                        title = s(R.string.dashboard_section_elevation_energy),
                        rows = buildList {
                            if (activity.elevationGainMeters != null && activity.elevationLossMeters != null) {
                                add(
                                    s(R.string.dashboard_label_elevation) to
                                        s(R.string.dashboard_elevation_balance, activity.elevationGainMeters, activity.elevationLossMeters)
                                )
                            }
                            if (altitudePoints.isNotEmpty()) {
                                add(
                                    s(R.string.dashboard_label_elevation_profile) to
                                        s(
                                            R.string.dashboard_elevation_profile_range,
                                            altitudePoints.minOrNull()?.toWholeNumber() ?: 0,
                                            altitudePoints.maxOrNull()?.toWholeNumber() ?: 0,
                                        )
                                )
                            }
                            activity.caloriesBurned?.let { add(s(R.string.dashboard_label_calories) to s(R.string.dashboard_calories_value, it.toWholeNumber())) }
                        }
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
                            add(
                                DetailSectionActionUiModel(
                                    label = s(R.string.dashboard_action_share),
                                    type = DetailSectionActionType.SHARE,
                                )
                            )
                            if (trackPoints.isNotEmpty()) {
                                add(
                                    DetailSectionActionUiModel(
                                        label = s(R.string.dashboard_action_map),
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
            title = bike.driveUnit?.productName ?: s(R.string.dashboard_bike_fallback_title),
            subtitle = bike.headUnit?.productName,
            odometerLabel = bike.driveUnit?.odometerMeters
                ?.div(1000.0)
                ?.let { String.format(Locale.US, "%.1f km", it) },
            assistSpeedLabel = bike.driveUnit?.maximumAssistanceSpeedKmh?.toSpeedText(),
            batterySummary = bike.batteries.firstOrNull()?.let { battery ->
                battery.totalChargeCycles?.let {
                    s(
                        R.string.dashboard_battery_cycles,
                        battery.productName ?: s(R.string.dashboard_battery_fallback_title),
                        String.format(Locale.US, "%.1f", it),
                    )
                } ?: (battery.productName ?: s(R.string.dashboard_battery_fallback_title))
            }
        )
    }

    private fun toBikeDetailUiModel(bike: BoschBike): BikeDetailUiModel {
        return BikeDetailUiModel(
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
                            bike.driveUnit?.odometerMeters?.div(1000.0)
                                ?.let { add(s(R.string.dashboard_label_odometer) to String.format(Locale.US, "%.1f km", it)) }
                            bike.driveUnit?.maximumAssistanceSpeedKmh
                                ?.let { add(s(R.string.dashboard_label_max_assist) to it.toSpeedText()) }
                            bike.driveUnit?.rearWheelCircumferenceMillimeters
                                ?.let { add(s(R.string.dashboard_label_wheel_circumference) to s(R.string.dashboard_wheel_circumference_value, it.toWholeNumber())) }
                        }
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
                                driveUnit.walkAssistMaximumSpeedKmh
                                    ?.let { add(s(R.string.dashboard_label_walk_assist_max) to it.toSpeedText()) }
                                driveUnit.totalPowerOnHours?.let { add(s(R.string.dashboard_label_total_power_on_hours) to s(R.string.dashboard_hours_value, it)) }
                                driveUnit.supportPowerOnHours?.let { add(s(R.string.dashboard_label_support_power_on_hours) to s(R.string.dashboard_hours_value, it)) }
                                if (driveUnit.activeAssistModes.isNotEmpty()) {
                                    add(s(R.string.dashboard_label_assist_modes) to driveUnit.activeAssistModes.toAssistModeSummary())
                                }
                            }
                        )
                    )
                }

                if (bike.batteries.isNotEmpty()) {
                    add(
                        DetailSectionUiModel(
                            title = s(R.string.dashboard_section_batteries),
                            rows = bike.batteries.flatMapIndexed { index, battery ->
                                battery.toRows(prefix = s(R.string.dashboard_battery_prefix, index + 1))
                            }
                        )
                    )
                }

                bike.remoteControl?.let { remote ->
                    add(
                        DetailSectionUiModel(
                            title = s(R.string.dashboard_section_remote),
                            rows = remote.toRows()
                        )
                    )
                }

                bike.headUnit?.let { headUnit ->
                    add(
                        DetailSectionUiModel(
                            title = s(R.string.dashboard_section_head_unit),
                            rows = headUnit.toRows()
                        )
                    )
                }
            }
        )
    }

    private fun BoschBattery.toRows(prefix: String): List<Pair<String, String>> = buildList {
        productName?.let { add(s(R.string.dashboard_battery_prefix_product, prefix) to it) }
        partNumber?.let { add(s(R.string.dashboard_battery_prefix_part_number, prefix) to it) }
        serialNumber?.let { add(s(R.string.dashboard_battery_prefix_serial_number, prefix) to it) }
        deliveredWhOverLifetime?.let { add(s(R.string.dashboard_battery_prefix_energy, prefix) to s(R.string.dashboard_wh_value, it)) }
        totalChargeCycles?.let { add(s(R.string.dashboard_battery_prefix_total_cycles, prefix) to s(R.string.dashboard_cycles_value, String.format(Locale.US, "%.1f", it))) }
        onBikeChargeCycles?.let { add(s(R.string.dashboard_battery_prefix_on_bike_cycles, prefix) to s(R.string.dashboard_cycles_value, String.format(Locale.US, "%.1f", it))) }
        offBikeChargeCycles?.let { add(s(R.string.dashboard_battery_prefix_off_bike_cycles, prefix) to s(R.string.dashboard_cycles_value, String.format(Locale.US, "%.1f", it))) }
    }

    private fun info.meuse24.m24bikestats.domain.model.BoschComponent.toRows(): List<Pair<String, String>> = buildList {
        productName?.let { add(s(R.string.dashboard_label_product) to it) }
        partNumber?.let { add(s(R.string.dashboard_label_part_number) to it) }
        serialNumber?.let { add(s(R.string.dashboard_label_serial_number) to it) }
    }

    private fun List<BoschAssistMode>.toAssistModeSummary(): String =
        joinToString(" | ") { mode ->
            mode.reachableRangeKm?.let { s(R.string.dashboard_assist_mode_range, mode.name, it.toWholeNumber()) } ?: mode.name
        }

    private fun String.toReadableDateTime(): String {
        return runCatching {
            Instant.parse(this)
                .atZone(ZoneId.systemDefault())
                .format(DATE_TIME_FORMATTER)
        }.getOrDefault(this)
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

    private fun s(@StringRes resId: Int, vararg args: Any): String =
        stringResolver.get(resId, args)

    private fun Double.toWholeNumber(): String =
        String.format(Locale.US, "%.0f", this)

    private fun Double.toCoordinateText(): String =
        String.format(Locale.US, "%.5f", this)

    private fun BoschActivityDetailPoint.hasCoordinates(): Boolean {
        val latitude = latitude ?: return false
        val longitude = longitude ?: return false
        return latitude != 0.0 || longitude != 0.0
    }

    private fun filterPresentedActivities(
        activities: List<ActivityCardUiModel>,
        searchQuery: String,
        dateRangeFilter: ActivityDateRangeFilter,
        sortOption: ActivitySortOption,
    ): List<ActivityCardUiModel> = filterAndSortActivities(
        activities = activities,
        searchQuery = searchQuery,
        dateRangeFilter = dateRangeFilter,
        sortOption = sortOption,
    )

    companion object {
        private const val ACTIVITIES_PAGE_SIZE = 20
        private val DATE_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        private val EXPORT_DATE_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    }
}
