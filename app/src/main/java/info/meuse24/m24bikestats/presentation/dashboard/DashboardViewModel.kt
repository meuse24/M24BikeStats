package info.meuse24.m24bikestats.presentation.dashboard

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.meuse24.m24bikestats.R
import info.meuse24.m24bikestats.domain.model.CsvSeparator
import info.meuse24.m24bikestats.domain.usecase.ExportSmartSystemActivityDetailsCsvUseCase
import info.meuse24.m24bikestats.domain.usecase.ExportSmartSystemActivitiesCsvUseCase
import info.meuse24.m24bikestats.domain.usecase.GetSmartSystemActivitiesUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveAppSettingsUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemActivitiesUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemBikesUseCase
import info.meuse24.m24bikestats.domain.usecase.RefreshSmartSystemActivitiesUseCase
import info.meuse24.m24bikestats.domain.usecase.RefreshSmartSystemBikesUseCase
import info.meuse24.m24bikestats.domain.usecase.SyncSmartSystemCloudUseCase
import info.meuse24.m24bikestats.domain.usecase.UpdateCsvSeparatorUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DashboardViewModel(
    private val observeCachedActivities: ObserveCachedSmartSystemActivitiesUseCase,
    private val observeCachedBikes: ObserveCachedSmartSystemBikesUseCase,
    private val getActivities: GetSmartSystemActivitiesUseCase,
    private val refreshActivitiesUseCase: RefreshSmartSystemActivitiesUseCase,
    private val exportActivitiesCsv: ExportSmartSystemActivitiesCsvUseCase,
    private val exportActivityDetailsCsv: ExportSmartSystemActivityDetailsCsvUseCase,
    private val refreshBikesUseCase: RefreshSmartSystemBikesUseCase,
    private val syncSmartSystemCloudUseCase: SyncSmartSystemCloudUseCase,
    private val observeAppSettings: ObserveAppSettingsUseCase,
    private val updateCsvSeparatorUseCase: UpdateCsvSeparatorUseCase,
    private val detailActionHandler: DashboardDetailActionHandler,
    private val uiModelMapper: DashboardUiModelMapper,
    private val stringResolver: DashboardStringResolver,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(isInitialLoading = true))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var activityOffset: Int = 0
    private var activityTotalCount: Int = 0

    init {
        observeActivities()
        observeBikes()
        observeSettings()
        refresh(force = false)
    }

    private fun observeActivities() {
        viewModelScope.launch {
            observeCachedActivities().collectLatest { activities ->
                val allActivities = activities.map(uiModelMapper::toActivityCardUiModel)
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
                        bikes = bikes.map(uiModelMapper::toBikeCardUiModel),
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
        detailActionHandler.loadBikeDetail(
            scope = viewModelScope,
            bikeId = bikeId,
            force = false,
            currentState = _uiState::value,
            updateState = _uiState::update,
        )
    }

    fun refreshBikeDetail(bikeId: String) {
        detailActionHandler.loadBikeDetail(
            scope = viewModelScope,
            bikeId = bikeId,
            force = true,
            currentState = _uiState::value,
            updateState = _uiState::update,
        )
    }

    fun loadActivityDetail(activityId: String) {
        detailActionHandler.loadActivityDetail(
            scope = viewModelScope,
            activityId = activityId,
            force = false,
            currentState = _uiState::value,
            updateState = _uiState::update,
        )
    }

    fun refreshActivityDetail(activityId: String) {
        detailActionHandler.loadActivityDetail(
            scope = viewModelScope,
            activityId = activityId,
            force = true,
            currentState = _uiState::value,
            updateState = _uiState::update,
        )
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
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

    private fun s(@StringRes resId: Int, vararg args: Any): String =
        stringResolver.get(resId, args)

    private companion object {
        private const val ACTIVITIES_PAGE_SIZE = 20
        private val EXPORT_DATE_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    }
}
