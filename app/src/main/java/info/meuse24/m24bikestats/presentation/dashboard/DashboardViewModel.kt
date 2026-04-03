package info.meuse24.m24bikestats.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.meuse24.m24bikestats.domain.model.CsvSeparator
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

    fun updateCsvSeparator(separator: CsvSeparator) {
        feedHandler.updateCsvSeparator(
            scope = viewModelScope,
            currentState = _uiState::value,
            separator = separator,
        )
    }

    fun syncCloudData() {
        operationsHandler.syncCloudData(
            scope = viewModelScope,
            currentState = _uiState::value,
            updateState = _uiState::update,
        )
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
