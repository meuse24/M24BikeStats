package info.meuse24.m24bikestats.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.meuse24.m24bikestats.domain.usecase.GetStatisticsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class StatisticsViewModel(
    getStatisticsUseCase: GetStatisticsUseCase,
    private val uiModelMapper: StatisticsUiModelMapper,
) : ViewModel() {

    private val selectionState = MutableStateFlow(StatisticsSelectionState())

    val uiState: StateFlow<StatisticsUiState> = combine(
        getStatisticsUseCase(),
        selectionState,
    ) { activities, selectionState ->
        val grouping = selectionState.grouping
        val totalTours = activities.size
        val totalDistanceKm = activities.sumOf { it.distanceMeters } / 1000.0
        val totalDurationHours = activities.sumOf { it.durationWithoutStopsSeconds }.toDouble() / 3600.0
        val periods = uiModelMapper.mapPeriods(
            activities = activities,
            grouping = grouping,
        )
        val highlights = uiModelMapper.mapHighlights(
            activities = activities,
            totalDistanceKm = totalDistanceKm,
            totalDurationHours = totalDurationHours,
        )
        val selectedPeriod = periods.firstOrNull { it.startEpochMillis == selectionState.selectedPeriodStart }
        toUiState(
            grouping = grouping,
            periods = periods,
            selectedPeriod = selectedPeriod,
            highlights = highlights,
            totalTours = totalTours,
            totalDistanceKm = totalDistanceKm,
            totalDurationHours = totalDurationHours,
        )
    }.distinctUntilChanged().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatisticsUiState(isLoading = true),
    )

    fun updateGrouping(grouping: StatisticsGrouping) {
        selectionState.update { current ->
            current.copy(
                grouping = grouping,
                selectedPeriodStart = null,
            )
        }
    }

    fun toggleSelectedPeriod(startEpochMillis: Long) {
        selectionState.update { current ->
            current.copy(
                selectedPeriodStart = if (current.selectedPeriodStart == startEpochMillis) {
                    null
                } else {
                    startEpochMillis
                },
            )
        }
    }

    private fun toUiState(
        grouping: StatisticsGrouping,
        periods: List<PeriodStats>,
        selectedPeriod: PeriodStats?,
        highlights: StatisticsHighlights?,
        totalTours: Int,
        totalDistanceKm: Double,
        totalDurationHours: Double,
    ): StatisticsUiState {
        return StatisticsUiState(
            periods = periods,
            selectedPeriod = selectedPeriod,
            grouping = grouping,
            totalTours = totalTours,
            totalDistanceKm = totalDistanceKm,
            totalDurationHours = totalDurationHours,
            avgDistanceKm = if (totalTours > 0) totalDistanceKm / totalTours else 0.0,
            avgDurationHours = if (totalTours > 0) totalDurationHours / totalTours else 0.0,
            highlights = highlights,
            isLoading = false,
        )
    }

    private data class StatisticsSelectionState(
        val grouping: StatisticsGrouping = StatisticsGrouping.MONTH,
        val selectedPeriodStart: Long? = null,
    )
}
