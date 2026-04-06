package info.meuse24.m24bikestats.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.meuse24.m24bikestats.domain.model.BoschActivity
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

    private val grouping = MutableStateFlow(StatisticsGrouping.MONTH)
    private val selectedPeriodStart = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<StatisticsUiState> = combine(
        getStatisticsUseCase(),
        grouping,
        selectedPeriodStart,
    ) { activities, grouping, selectedPeriodStart ->
        val periods = uiModelMapper.mapPeriods(
            activities = activities,
            grouping = grouping,
        )
        val selectedPeriod = periods.firstOrNull { it.startEpochMillis == selectedPeriodStart }
        activities.toUiState(
            grouping = grouping,
            periods = periods,
            selectedPeriod = selectedPeriod,
        )
    }.distinctUntilChanged().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatisticsUiState(isLoading = true),
    )

    fun updateGrouping(grouping: StatisticsGrouping) {
        this.grouping.update { grouping }
        selectedPeriodStart.update { null }
    }

    fun toggleSelectedPeriod(startEpochMillis: Long) {
        selectedPeriodStart.update { current ->
            if (current == startEpochMillis) null else startEpochMillis
        }
    }

    private fun List<BoschActivity>.toUiState(
        grouping: StatisticsGrouping,
        periods: List<PeriodStats>,
        selectedPeriod: PeriodStats?,
    ): StatisticsUiState =
        StatisticsUiState(
            periods = periods,
            selectedPeriod = selectedPeriod,
            grouping = grouping,
            totalTours = size,
            totalDistanceKm = sumOf { it.distanceMeters } / 1000.0,
            totalDurationHours = sumOf { it.durationWithoutStopsSeconds }.toDouble() / 3600.0,
            isLoading = false,
        )
}
