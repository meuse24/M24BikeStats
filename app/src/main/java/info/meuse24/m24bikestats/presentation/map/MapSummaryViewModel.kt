package info.meuse24.m24bikestats.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.meuse24.m24bikestats.domain.usecase.GetActivityMapPointsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MapSummaryViewModel(
    getMapPoints: GetActivityMapPointsUseCase,
) : ViewModel() {

    val uiState = getMapPoints()
        .map { points ->
            MapSummaryUiState(
                points = points,
                isLoading = false,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MapSummaryUiState(),
        )
}
