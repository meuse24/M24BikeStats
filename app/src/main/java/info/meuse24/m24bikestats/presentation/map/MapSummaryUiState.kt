package info.meuse24.m24bikestats.presentation.map

import info.meuse24.m24bikestats.domain.model.ActivityMapPoint

data class MapSummaryUiState(
    val points: List<ActivityMapPoint> = emptyList(),
    val isLoading: Boolean = true,
)
