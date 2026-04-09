package info.meuse24.m24bikestats.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.meuse24.m24bikestats.domain.model.CloudSyncDetailMode
import info.meuse24.m24bikestats.domain.model.CsvExportFormat
import info.meuse24.m24bikestats.domain.model.BackgroundSyncMode
import info.meuse24.m24bikestats.domain.model.DisplayMode
import info.meuse24.m24bikestats.domain.model.ExplanationTextsPromptTiming
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DashboardViewModel(
    private val feedHandler: DashboardFeedHandler,
    private val operationsHandler: DashboardOperationsHandler,
    private val detailActionHandler: DashboardDetailActionHandler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(isInitialLoading = true))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        feedHandler.startObserving(
            scope = viewModelScope,
            currentState = _uiState::value,
            updateState = _uiState::update,
        )
        refresh(force = false)
    }

    fun refresh(force: Boolean = true) {
        feedHandler.refresh(
            scope = viewModelScope,
            force = force,
            currentState = _uiState::value,
            updateState = _uiState::update,
        )
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

    fun updateCloudSyncDetailMode(mode: CloudSyncDetailMode) {
        feedHandler.updateCloudSyncDetailMode(
            scope = viewModelScope,
            currentState = _uiState::value,
            mode = mode,
        )
    }

    fun updateBackgroundSyncMode(mode: BackgroundSyncMode) {
        feedHandler.updateBackgroundSyncMode(
            scope = viewModelScope,
            currentState = _uiState::value,
            mode = mode,
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

    fun syncCloudData() {
        operationsHandler.syncCloudData(
            scope = viewModelScope,
            currentState = _uiState::value,
            updateState = _uiState::update,
        )
    }

    fun loadMissingActivityDetails() {
        operationsHandler.loadMissingActivityDetails(
            scope = viewModelScope,
            currentState = _uiState::value,
            updateState = _uiState::update,
        )
    }

    fun refreshStaleActivityDetails() {
        operationsHandler.refreshStaleActivityDetails(
            scope = viewModelScope,
            currentState = _uiState::value,
            updateState = _uiState::update,
        )
    }

    fun cancelCloudSync() {
        operationsHandler.cancelCloudSync(_uiState::update)
    }

    fun cancelPendingActivityDetailsSync() {
        operationsHandler.cancelPendingActivityDetailsSync(_uiState::update)
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
}
