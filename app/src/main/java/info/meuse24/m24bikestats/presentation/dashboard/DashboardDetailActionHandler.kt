package info.meuse24.m24bikestats.presentation.dashboard

import androidx.annotation.StringRes
import info.meuse24.m24bikestats.auth.OidcDiscoveryInfoProvider
import info.meuse24.m24bikestats.auth.OidcDiscoveryInfoUiModel
import info.meuse24.m24bikestats.auth.OidcCertificateInfoProvider
import info.meuse24.m24bikestats.auth.OidcCertificateInfoUiModel
import info.meuse24.m24bikestats.auth.OidcUserInfoProvider
import info.meuse24.m24bikestats.auth.OidcUserInfoUiModel
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
import kotlinx.coroutines.async
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
    private val oidcCertificateInfoProvider: OidcCertificateInfoProvider,
    private val oidcUserInfoProvider: OidcUserInfoProvider,
    private val oidcDiscoveryInfoProvider: OidcDiscoveryInfoProvider,
    private val uiModelMapper: DashboardUiModelMapper,
    private val stringResolver: DashboardStringResolver,
) {
    private var activityDetailLoadJob: Job? = null
    private var bikeDetailLoadJob: Job? = null
    private var activityDetailObservationJob: Job? = null
    private var bikeDetailObservationJob: Job? = null

    fun cancelAllLoads() {
        activityDetailLoadJob?.cancel()
        bikeDetailLoadJob?.cancel()
        activityDetailObservationJob?.cancel()
        bikeDetailObservationJob?.cancel()
        activityDetailLoadJob = null
        bikeDetailLoadJob = null
        activityDetailObservationJob = null
        bikeDetailObservationJob = null
    }

    suspend fun cancelAndJoinAllLoads() {
        val jobs = listOfNotNull(
            activityDetailLoadJob,
            bikeDetailLoadJob,
            activityDetailObservationJob,
            bikeDetailObservationJob,
        )
        jobs.forEach { it.cancel() }
        activityDetailLoadJob = null
        bikeDetailLoadJob = null
        activityDetailObservationJob = null
        bikeDetailObservationJob = null
        jobs.forEach { it.join() }
    }

    fun loadBikeDetail(
        scope: CoroutineScope,
        bikeId: String,
        currentState: () -> DashboardUiState,
        updateState: ((DashboardUiState) -> DashboardUiState) -> Unit,
    ) {
        bikeDetailLoadJob?.cancel()
        bikeDetailObservationJob?.cancel()
        bikeDetailLoadJob = scope.launch {
            val cachedBike = getCachedBike(bikeId)
            updateState {
                it.copy(
                    selectedBikeId = bikeId,
                    selectedBikeDetail = cachedBike?.toBikeDetailUiModel(),
                    isBikeDetailLoading = cachedBike == null,
                    isBikeDetailRefreshing = cachedBike != null,
                    error = null,
                )
            }

            val certificateDeferred = async { oidcCertificateInfoProvider.loadCurrentCertificate() }
            val userInfoDeferred = async { oidcUserInfoProvider.loadCurrentUserInfo() }
            val discoveryDeferred = async { oidcDiscoveryInfoProvider.loadCurrentDiscovery() }
            val oidcCertificate = certificateDeferred.await()
            val oidcUserInfo = userInfoDeferred.await()
            val oidcDiscoveryInfo = discoveryDeferred.await()
            if (currentState().selectedBikeId == bikeId) {
                val currentBike = getCachedBike(bikeId) ?: cachedBike
                updateState {
                    it.copy(
                        selectedBikeDetail = currentBike?.toBikeDetailUiModel(
                            oidcCertificateInfo = oidcCertificate,
                            oidcUserInfo = oidcUserInfo,
                            oidcDiscoveryInfo = oidcDiscoveryInfo,
                        ),
                    )
                }
            }

            bikeDetailObservationJob = scope.launch {
                observeCachedBikeDetail(bikeId).collectLatest { bike ->
                    if (currentState().selectedBikeId != bikeId) return@collectLatest
                    updateState {
                        it.copy(
                            selectedBikeDetail = bike?.toBikeDetailUiModel(
                                oidcCertificateInfo = oidcCertificate,
                                oidcUserInfo = oidcUserInfo,
                                oidcDiscoveryInfo = oidcDiscoveryInfo,
                            ),
                            isBikeDetailLoading = bike == null && it.isBikeDetailLoading,
                        )
                    }
                }
            }

            refreshBikeDetailUseCase(bikeId, force = false).getOrElse { error ->
                updateState {
                    it.copy(
                        isBikeDetailLoading = false,
                        isBikeDetailRefreshing = false,
                        selectedBikeDetail = cachedBike?.toBikeDetailUiModel(
                            oidcCertificateInfo = oidcCertificate,
                            oidcUserInfo = oidcUserInfo,
                            oidcDiscoveryInfo = oidcDiscoveryInfo,
                        ),
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
        currentState: () -> DashboardUiState,
        updateState: ((DashboardUiState) -> DashboardUiState) -> Unit,
    ) {
        activityDetailLoadJob?.cancel()
        activityDetailObservationJob?.cancel()
        activityDetailLoadJob = scope.launch {
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

            refreshActivityDetailUseCase(activityId, force = false).getOrElse { error ->
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

    private fun info.meuse24.m24bikestats.domain.model.BoschBike.toBikeDetailUiModel(
        oidcCertificateInfo: OidcCertificateInfoUiModel? = null,
        oidcUserInfo: OidcUserInfoUiModel? = null,
        oidcDiscoveryInfo: OidcDiscoveryInfoUiModel? = null,
    ) = uiModelMapper.toBikeDetailUiModel(
        bike = this,
        oidcCertificateInfo = oidcCertificateInfo,
        oidcUserInfo = oidcUserInfo,
        oidcDiscoveryInfo = oidcDiscoveryInfo,
    )
}
