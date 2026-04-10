package info.meuse24.m24bikestats.presentation.dashboard

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.meuse24.m24bikestats.R
import info.meuse24.m24bikestats.auth.OidcCacheRepository
import info.meuse24.m24bikestats.data.local.database.BoschDatabase
import info.meuse24.m24bikestats.domain.model.CsvExportFormat
import info.meuse24.m24bikestats.domain.model.DisplayMode
import info.meuse24.m24bikestats.domain.model.ExplanationTextsPromptTiming
import info.meuse24.m24bikestats.domain.model.SmartSystemCloudSyncPhase
import info.meuse24.m24bikestats.domain.model.SmartSystemCloudSyncSummary
import info.meuse24.m24bikestats.domain.repository.AppSettingsRepository
import info.meuse24.m24bikestats.domain.usecase.RefreshSmartSystemDataUseCase
import info.meuse24.m24bikestats.domain.usecase.SyncSmartSystemCloudUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DashboardViewModel(
    private val feedHandler: DashboardFeedHandler,
    private val operationsHandler: DashboardOperationsHandler,
    private val detailActionHandler: DashboardDetailActionHandler,
    private val performInitialSyncUseCase: SyncSmartSystemCloudUseCase,
    private val refreshSmartSystemDataUseCase: RefreshSmartSystemDataUseCase,
    private val appSettingsRepository: AppSettingsRepository,
    private val oidcCacheRepository: OidcCacheRepository,
    private val database: BoschDatabase,
    private val stringResolver: DashboardStringResolver,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(isInitialLoading = true))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private var syncJob: Job? = null

    init {
        feedHandler.startObserving(
            scope = viewModelScope,
            currentState = _uiState::value,
            updateState = _uiState::update,
        )
        viewModelScope.launch {
            ensureInitialSyncState()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            if (appSettingsRepository.getSettings().initialSyncCompletedAtEpochMillis == 0L) {
                performInitialSync()
                return@launch
            }
            runSync(
                isInitialSync = false,
                action = refreshSmartSystemDataUseCase::invoke,
            )
        }
    }

    fun performInitialSync() {
        viewModelScope.launch {
            runSync(
                isInitialSync = true,
                action = performInitialSyncUseCase::invoke,
            )
        }
    }

    fun loadMoreActivities() {
        feedHandler.loadMoreActivities(
            scope = viewModelScope,
            currentState = _uiState::value,
            updateState = _uiState::update,
        )
    }

    fun exportAllActivitiesCsv() {
        operationsHandler.exportAllActivitiesCsv(
            scope = viewModelScope,
            currentState = _uiState::value,
            updateState = _uiState::update,
        )
    }

    fun exportVisibleActivityDetailsCsv() {
        operationsHandler.exportVisibleActivityDetailsCsv(
            scope = viewModelScope,
            currentState = _uiState::value,
            updateState = _uiState::update,
        )
    }

    fun exportPdfSummaryReport() {
        operationsHandler.exportPdfSummaryReport(
            scope = viewModelScope,
            currentState = _uiState::value,
            updateState = _uiState::update,
        )
    }

    fun cancelActivitiesCsvExport() {
        operationsHandler.cancelActivitiesCsvExport(_uiState::update)
    }

    fun cancelActivityDetailsCsvExport() {
        operationsHandler.cancelActivityDetailsCsvExport(_uiState::update)
    }

    fun cancelPdfExport() {
        operationsHandler.cancelPdfExport(_uiState::update)
    }

    fun updateActivityDateRangeFilter(filter: ActivityDateRangeFilter) {
        feedHandler.updateActivityDateRangeFilter(
            filter = filter,
            updateState = _uiState::update,
        )
    }

    fun updateActivitySortOption(sortOption: ActivitySortOption) {
        feedHandler.updateActivitySortOption(
            sortOption = sortOption,
            updateState = _uiState::update,
        )
    }

    fun updateActivitySearchQuery(searchQuery: String) {
        feedHandler.updateActivitySearchQuery(
            searchQuery = searchQuery,
            updateState = _uiState::update,
        )
    }

    fun onActivitiesCsvExportHandled() {
        operationsHandler.onActivitiesCsvExportHandled(_uiState::update)
    }

    fun onActivityDetailsCsvExportHandled() {
        operationsHandler.onActivityDetailsCsvExportHandled(_uiState::update)
    }

    fun onPdfExportHandled() {
        operationsHandler.onPdfExportHandled(_uiState::update)
    }

    fun updateCsvExportFormat(format: CsvExportFormat) {
        feedHandler.updateCsvExportFormat(
            scope = viewModelScope,
            currentState = _uiState::value,
            format = format,
        )
    }

    fun updateDisplayMode(mode: DisplayMode) {
        feedHandler.updateDisplayMode(
            scope = viewModelScope,
            currentState = _uiState::value,
            mode = mode,
        )
    }

    fun updateShowExplanationTexts(show: Boolean) {
        feedHandler.updateShowExplanationTexts(
            scope = viewModelScope,
            currentState = _uiState::value,
            show = show,
        )
    }

    fun updateExplanationTextsPromptTiming(timing: ExplanationTextsPromptTiming) {
        feedHandler.updateExplanationTextsPromptTiming(
            scope = viewModelScope,
            timing = timing,
        )
    }

    fun resetExplanationTextsPrompt() {
        feedHandler.resetExplanationTextsPrompt(scope = viewModelScope)
    }

    fun markExplanationTextsPromptHandled() {
        feedHandler.markExplanationTextsPromptHandled(scope = viewModelScope)
    }

    fun cancelCloudSync() {
        syncJob?.cancel()
        syncJob = null
        _uiState.update {
            it.copy(
                isInitialLoading = false,
                isRefreshing = false,
                isSyncingCloudData = false,
                syncPhase = null,
                syncPhaseLabel = null,
                syncLoadedActivityCount = 0,
                syncTotalActivityCount = 0,
                error = s(R.string.dashboard_info_sync_cancelled),
            )
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            syncJob?.cancel()
            syncJob = null
            operationsHandler.cancelAllOperationsSilently()
            detailActionHandler.cancelAllLoads()
            appSettingsRepository.resetInitialSyncFlag()
            appSettingsRepository.resetLatestCachedActivityStartTime()
            oidcCacheRepository.clearOidcCache()
            withContext(Dispatchers.IO) {
                database.clearAllTables()
            }

            val currentState = _uiState.value
            _uiState.value = DashboardUiState(
                isInitialLoading = true,
                csvExportFormat = currentState.csvExportFormat,
                displayMode = currentState.displayMode,
                showExplanationTexts = currentState.showExplanationTexts,
            )

            runSync(
                isInitialSync = true,
                action = performInitialSyncUseCase::invoke,
            )
        }
    }

    fun loadBikeDetail(bikeId: String) {
        detailActionHandler.loadBikeDetail(
            scope = viewModelScope,
            bikeId = bikeId,
            currentState = _uiState::value,
            updateState = _uiState::update,
        )
    }

    fun loadActivityDetail(activityId: String) {
        detailActionHandler.loadActivityDetail(
            scope = viewModelScope,
            activityId = activityId,
            currentState = _uiState::value,
            updateState = _uiState::update,
        )
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private suspend fun ensureInitialSyncState() {
        val settings = appSettingsRepository.getSettings()
        if (settings.initialSyncCompletedAtEpochMillis > 0L) {
            val cachedActivityCount = withContext(Dispatchers.IO) { database.activityDao().count() }
            if (cachedActivityCount == 0) {
                appSettingsRepository.resetInitialSyncFlag()
                appSettingsRepository.resetLatestCachedActivityStartTime()
            }
        }

        if (appSettingsRepository.getSettings().initialSyncCompletedAtEpochMillis == 0L) {
            runSync(
                isInitialSync = true,
                action = performInitialSyncUseCase::invoke,
            )
        } else {
            _uiState.update { it.copy(isInitialLoading = false) }
        }
    }

    private suspend fun runSync(
        isInitialSync: Boolean,
        action: suspend (((info.meuse24.m24bikestats.domain.model.SmartSystemCloudSyncProgress) -> Unit)) ->
            Result<SmartSystemCloudSyncSummary>,
    ) {
        if (syncJob?.isActive == true) return
        if (!isInitialSync && !_uiState.value.canRunBackgroundOperation()) return

        syncJob = viewModelScope.launch {
            val hasCacheContent = _uiState.value.loadedActivityCount > 0 || _uiState.value.bikes.isNotEmpty()
            _uiState.update {
                it.copy(
                    isInitialLoading = isInitialSync || !hasCacheContent,
                    isRefreshing = !isInitialSync && hasCacheContent,
                    isSyncingCloudData = true,
                    syncPhase = null,
                    syncPhaseLabel = null,
                    syncLoadedActivityCount = 0,
                    syncTotalActivityCount = 0,
                    error = null,
                )
            }

            val summary = action { progress ->
                _uiState.update {
                    it.copy(
                        syncPhase = progress.phase,
                        syncPhaseLabel = syncPhaseLabel(progress.phase),
                        syncLoadedActivityCount = progress.processedCount,
                        syncTotalActivityCount = progress.totalCount,
                    )
                }
            }.getOrElse { error ->
                if (error is CancellationException) {
                    _uiState.update {
                        it.copy(
                            isInitialLoading = false,
                            isRefreshing = false,
                            isSyncingCloudData = false,
                            syncPhase = null,
                            syncPhaseLabel = null,
                            syncLoadedActivityCount = 0,
                            syncTotalActivityCount = 0,
                            error = s(R.string.dashboard_info_sync_cancelled),
                        )
                    }
                    syncJob = null
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        isInitialLoading = false,
                        isRefreshing = false,
                        isSyncingCloudData = false,
                        syncPhase = null,
                        syncPhaseLabel = null,
                        syncLoadedActivityCount = 0,
                        syncTotalActivityCount = 0,
                        error = error.message ?: s(R.string.dashboard_error_cloud_sync),
                    )
                }
                syncJob = null
                return@launch
            }

            _uiState.update {
                it.copy(
                    isInitialLoading = false,
                    isRefreshing = false,
                    isSyncingCloudData = false,
                    syncPhase = null,
                    syncPhaseLabel = null,
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
            syncJob = null
        }
        syncJob?.join()
    }

    private fun syncPhaseLabel(phase: SmartSystemCloudSyncPhase): String = when (phase) {
        SmartSystemCloudSyncPhase.BIKES -> s(R.string.home_sync_phase_bikes)
        SmartSystemCloudSyncPhase.ACTIVITIES -> s(R.string.home_sync_phase_activities)
        SmartSystemCloudSyncPhase.ACTIVITY_DETAILS -> s(R.string.home_sync_phase_activity_details_missing_only)
    }

    private fun s(@StringRes resId: Int, vararg args: Any): String =
        stringResolver.get(resId, args)

    private companion object {
        private val EXPORT_DATE_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    }
}
