package info.meuse24.m24bikestats.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.meuse24.m24bikestats.domain.model.StatisticsGrouping
import info.meuse24.m24bikestats.domain.usecase.GetStatisticsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModel(
    getStatisticsUseCase: GetStatisticsUseCase,
    private val uiModelMapper: StatisticsUiModelMapper,
) : ViewModel() {

    private val selectionState = MutableStateFlow(StatisticsSelectionState())

    val uiState: StateFlow<StatisticsUiState> = selectionState
        .flatMapLatest { selection ->
            getStatisticsUseCase(selection.grouping).map { overview ->
                uiModelMapper.toUiState(
                    overview = overview,
                    grouping = selection.grouping,
                    selectedPeriodStart = selection.selectedPeriodStart,
                )
            }
        }
        .distinctUntilChanged()
        .stateIn(
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

    private data class StatisticsSelectionState(
        val grouping: StatisticsGrouping = StatisticsGrouping.MONTH,
        val selectedPeriodStart: Long? = null,
    )
}
