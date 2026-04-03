package info.meuse24.m24bikestats.presentation.dashboard

import androidx.annotation.StringRes
import info.meuse24.m24bikestats.R
import info.meuse24.m24bikestats.domain.usecase.GetCachedSmartSystemActivityDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.GetCachedSmartSystemActivityUseCase
import info.meuse24.m24bikestats.domain.usecase.GetCachedSmartSystemBikeUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemActivityDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemBikeDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.RefreshSmartSystemActivityDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.RefreshSmartSystemBikeDetailUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DashboardDetailActionHandler(
    private val observeCachedActivityDetail: ObserveCachedSmartSystemActivityDetailUseCase,
    private val observeCachedBikeDetail: ObserveCachedSmartSystemBikeDetailUseCase,
    private val getCachedActivity: GetCachedSmartSystemActivityUseCase,
    private val getCachedActivityDetail: GetCachedSmartSystemActivityDetailUseCase,
    private val getCachedBike: GetCachedSmartSystemBikeUseCase,
    private val refreshActivityDetailUseCase: RefreshSmartSystemActivityDetailUseCase,
    private val refreshBikeDetailUseCase: RefreshSmartSystemBikeDetailUseCase,
    private val uiModelMapper: DashboardUiModelMapper,
    private val stringResolver: DashboardStringResolver,
) {
    private var activityDetailObservationJob: Job? = null
    private var bikeDetailObservationJob: Job? = null

    fun loadBikeDetail(
        scope: CoroutineScope,
        bikeId: String,
        force: Boolean,
        currentState: () -> DashboardUiState,
        updateState: ((DashboardUiState) -> DashboardUiState) -> Unit,
    ) {
        bikeDetailObservationJob?.cancel()
        scope.launch {
            val cachedBike = getCachedBike(bikeId)
            updateState {
                it.copy(
                    selectedBikeId = bikeId,
                    selectedBikeDetail = cachedBike?.let(uiModelMapper::toBikeDetailUiModel),
                    isBikeDetailLoading = cachedBike == null,
                    isBikeDetailRefreshing = cachedBike != null,
                    error = null,
                )
            }

            bikeDetailObservationJob = scope.launch {
                observeCachedBikeDetail(bikeId).collectLatest { bike ->
                    if (currentState().selectedBikeId != bikeId) return@collectLatest
                    updateState {
                        it.copy(
                            selectedBikeDetail = bike?.let(uiModelMapper::toBikeDetailUiModel),
                            isBikeDetailLoading = bike == null && it.isBikeDetailLoading,
                        )
                    }
                }
            }

            refreshBikeDetailUseCase(bikeId, force = force).getOrElse { error ->
                updateState {
                    it.copy(
                        isBikeDetailLoading = false,
                        isBikeDetailRefreshing = false,
                        selectedBikeDetail = cachedBike?.let(uiModelMapper::toBikeDetailUiModel),
                        error = error.message ?: s(R.string.dashboard_error_bike_detail_load),
                    )
                }
                return@launch
            }

            updateState {
                it.copy(
                    isBikeDetailLoading = false,
                    isBikeDetailRefreshing = false,
                    error = null,
                )
            }
        }
    }

    fun loadActivityDetail(
        scope: CoroutineScope,
        activityId: String,
        force: Boolean,
        currentState: () -> DashboardUiState,
        updateState: ((DashboardUiState) -> DashboardUiState) -> Unit,
    ) {
        activityDetailObservationJob?.cancel()
        scope.launch {
            val activity = getCachedActivity(activityId)
            if (activity == null) {
                updateState { it.copy(error = s(R.string.dashboard_error_activity_not_found)) }
                return@launch
            }

            val cachedDetail = getCachedActivityDetail(activityId)
            updateState {
                it.copy(
                    selectedActivityId = activityId,
                    selectedActivityDetail = cachedDetail?.let { detail -> uiModelMapper.toActivityDetailUiModel(activity, detail) },
                    isActivityDetailLoading = cachedDetail == null,
                    isActivityDetailRefreshing = cachedDetail != null,
                    error = null,
                )
            }

            activityDetailObservationJob = scope.launch {
                observeCachedActivityDetail(activityId).collectLatest { detail ->
                    if (currentState().selectedActivityId != activityId) return@collectLatest
                    updateState {
                        it.copy(
                            selectedActivityDetail = detail?.let { cached -> uiModelMapper.toActivityDetailUiModel(activity, cached) },
                            isActivityDetailLoading = detail == null && it.isActivityDetailLoading,
                        )
                    }
                }
            }

            refreshActivityDetailUseCase(activityId, force = force).getOrElse { error ->
                updateState {
                    it.copy(
                        isActivityDetailLoading = false,
                        isActivityDetailRefreshing = false,
                        selectedActivityDetail = cachedDetail?.let { detail -> uiModelMapper.toActivityDetailUiModel(activity, detail) },
                        error = error.message ?: s(R.string.dashboard_error_activity_detail_load),
                    )
                }
                return@launch
            }

            updateState {
                it.copy(
                    isActivityDetailLoading = false,
                    isActivityDetailRefreshing = false,
                    error = null,
                )
            }
        }
    }

    private fun s(@StringRes resId: Int, vararg args: Any): String =
        stringResolver.get(resId, args)
}
