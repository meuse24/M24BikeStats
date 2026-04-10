package info.meuse24.m24bikestats.presentation.dashboard

import androidx.annotation.StringRes
import info.meuse24.m24bikestats.auth.OidcCertificateInfoProvider
import info.meuse24.m24bikestats.R
import info.meuse24.m24bikestats.domain.model.CsvExportFormat
import info.meuse24.m24bikestats.domain.model.DisplayMode
import info.meuse24.m24bikestats.domain.model.ExplanationTextsPromptTiming
import info.meuse24.m24bikestats.domain.usecase.GetSmartSystemActivitiesUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveDataStatusOverviewUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveAppSettingsUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemActivityDetailCacheOverviewUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemActivitiesUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemBikesUseCase
import info.meuse24.m24bikestats.domain.usecase.UpdateCsvExportFormatUseCase
import info.meuse24.m24bikestats.domain.usecase.UpdateDisplayModeUseCase
import info.meuse24.m24bikestats.domain.usecase.UpdateExplanationTextsPromptTimingUseCase
import info.meuse24.m24bikestats.domain.usecase.ResetExplanationTextsPromptUseCase
import info.meuse24.m24bikestats.domain.usecase.MarkExplanationTextsPromptHandledUseCase
import info.meuse24.m24bikestats.domain.usecase.UpdateShowExplanationTextsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DashboardFeedHandler(
    private val observeCachedActivities: ObserveCachedSmartSystemActivitiesUseCase,
    private val observeCachedBikes: ObserveCachedSmartSystemBikesUseCase,
    private val observeCachedActivityDetailCacheOverview: ObserveCachedSmartSystemActivityDetailCacheOverviewUseCase,
    private val observeDataStatusOverview: ObserveDataStatusOverviewUseCase,
    private val observeAppSettings: ObserveAppSettingsUseCase,
    private val getActivities: GetSmartSystemActivitiesUseCase,
    private val updateCsvExportFormatUseCase: UpdateCsvExportFormatUseCase,
    private val updateDisplayModeUseCase: UpdateDisplayModeUseCase,
    private val updateExplanationTextsPromptTimingUseCase: UpdateExplanationTextsPromptTimingUseCase,
    private val resetExplanationTextsPromptUseCase: ResetExplanationTextsPromptUseCase,
    private val markExplanationTextsPromptHandledUseCase: MarkExplanationTextsPromptHandledUseCase,
    private val updateShowExplanationTextsUseCase: UpdateShowExplanationTextsUseCase,
    private val oidcCertificateInfoProvider: OidcCertificateInfoProvider,
    private val uiModelMapper: DashboardUiModelMapper,
    private val stringResolver: DashboardStringResolver,
) {
    private var activityOffset: Int = 0
    private var activityTotalCount: Int = 0

    fun startObserving(
        scope: CoroutineScope,
        currentState: () -> DashboardUiState,
        updateState: ((DashboardUiState) -> DashboardUiState) -> Unit,
    ) {
        scope.launch {
            observeCachedActivities().collectLatest { activities ->
                val allActivities = activities.map(uiModelMapper::toActivityCardUiModel)
                val presentedActivities = filterPresentedActivities(
                    activities = allActivities,
                    searchQuery = currentState().activitySearchQuery,
                    dateRangeFilter = currentState().activityDateRangeFilter,
                    sortOption = currentState().activitySortOption,
                )
                updateState { current ->
                    val resolvedTotal = when {
                        current.activityTotalCount > 0 -> maxOf(current.activityTotalCount, activities.size)
                        activityTotalCount > 0 -> maxOf(activityTotalCount, activities.size)
                        else -> activities.size
                    }
                    activityOffset = activities.size
                    val hasInitialContent = activities.isNotEmpty() || current.bikes.isNotEmpty()
                    current.copy(
                        isInitialLoading = current.isInitialLoading && !hasInitialContent,
                        allActivities = allActivities,
                        activities = presentedActivities,
                        loadedActivityCount = activities.size,
                        visibleActivityCount = presentedActivities.size,
                        activityTotalCount = resolvedTotal,
                        canLoadMoreActivities = activities.size < resolvedTotal,
                    )
                }
            }
        }

        scope.launch {
            observeCachedBikes().collectLatest { bikes ->
                updateState { current ->
                    val hasInitialContent = current.allActivities.isNotEmpty() || bikes.isNotEmpty()
                    current.copy(
                        isInitialLoading = current.isInitialLoading && !hasInitialContent,
                        bikes = bikes.map(uiModelMapper::toBikeCardUiModel),
                    )
                }
            }
        }

        scope.launch {
            observeCachedActivityDetailCacheOverview().collectLatest { overview ->
                updateState { current ->
                    current.copy(
                        cachedDetailActivityCount = overview.detailedActivityCount,
                        cachedDetailPointCount = overview.detailPointCount,
                        cachedGpsPointCount = overview.gpsPointCount,
                    )
                }
            }
        }

        scope.launch {
            observeDataStatusOverview().collectLatest { overview ->
                updateState { current ->
                    current.copy(
                        dataStatus = uiModelMapper.toDataStatusUiModel(overview),
                    )
                }
            }
        }

        scope.launch {
            observeAppSettings().collectLatest { settings ->
                updateState { current ->
                    current.copy(
                        csvExportFormat = settings.csvExportFormat,
                        displayMode = settings.displayMode,
                        showExplanationTexts = settings.showExplanationTexts,
                    )
                }
            }
        }

        scope.launch {
            val hasOidcCertificateInfo = oidcCertificateInfoProvider.loadCurrentCertificate() != null
            updateState { current ->
                current.copy(
                    hasOidcCertificateInfo = hasOidcCertificateInfo,
                )
            }
        }
    }

    fun loadMoreActivities(
        scope: CoroutineScope,
        currentState: () -> DashboardUiState,
        updateState: ((DashboardUiState) -> DashboardUiState) -> Unit,
    ) {
        val state = currentState()
        if (state.isInitialLoading || state.isRefreshing || state.isLoadingMoreActivities || !state.canLoadMoreActivities) return

        scope.launch {
            updateState { it.copy(isLoadingMoreActivities = true, error = null) }

            val nextPage = getActivities(limit = ACTIVITIES_PAGE_SIZE, offset = activityOffset)
                .getOrElse { error ->
                    updateState {
                        it.copy(
                            isLoadingMoreActivities = false,
                            error = error.message ?: s(R.string.dashboard_error_more_activities_load),
                        )
                    }
                    return@launch
                }

            activityOffset = nextPage.offset + nextPage.items.size
            activityTotalCount = nextPage.total

            updateState {
                it.copy(
                    isLoadingMoreActivities = false,
                    activityTotalCount = activityTotalCount,
                )
            }
        }
    }

    fun updateActivityDateRangeFilter(
        filter: ActivityDateRangeFilter,
        updateState: ((DashboardUiState) -> DashboardUiState) -> Unit,
    ) {
        updateState { current ->
            val presentedActivities = filterPresentedActivities(
                activities = current.allActivities,
                searchQuery = current.activitySearchQuery,
                dateRangeFilter = filter,
                sortOption = current.activitySortOption,
            )
            current.copy(
                activityDateRangeFilter = filter,
                activities = presentedActivities,
                visibleActivityCount = presentedActivities.size,
            )
        }
    }

    fun updateActivitySortOption(
        sortOption: ActivitySortOption,
        updateState: ((DashboardUiState) -> DashboardUiState) -> Unit,
    ) {
        updateState { current ->
            val presentedActivities = filterPresentedActivities(
                activities = current.allActivities,
                searchQuery = current.activitySearchQuery,
                dateRangeFilter = current.activityDateRangeFilter,
                sortOption = sortOption,
            )
            current.copy(
                activitySortOption = sortOption,
                activities = presentedActivities,
                visibleActivityCount = presentedActivities.size,
            )
        }
    }

    fun updateActivitySearchQuery(
        searchQuery: String,
        updateState: ((DashboardUiState) -> DashboardUiState) -> Unit,
    ) {
        updateState { current ->
            val presentedActivities = filterPresentedActivities(
                activities = current.allActivities,
                searchQuery = searchQuery,
                dateRangeFilter = current.activityDateRangeFilter,
                sortOption = current.activitySortOption,
            )
            current.copy(
                activitySearchQuery = searchQuery,
                activities = presentedActivities,
                visibleActivityCount = presentedActivities.size,
            )
        }
    }

    fun updateCsvExportFormat(
        scope: CoroutineScope,
        currentState: () -> DashboardUiState,
        format: CsvExportFormat,
    ) {
        if (currentState().csvExportFormat == format) return
        scope.launch {
            updateCsvExportFormatUseCase(format)
        }
    }

    fun updateDisplayMode(
        scope: CoroutineScope,
        currentState: () -> DashboardUiState,
        mode: DisplayMode,
    ) {
        if (currentState().displayMode == mode) return
        scope.launch {
            updateDisplayModeUseCase(mode)
        }
    }

    fun updateShowExplanationTexts(
        scope: CoroutineScope,
        currentState: () -> DashboardUiState,
        show: Boolean,
    ) {
        if (currentState().showExplanationTexts == show) return
        scope.launch {
            updateShowExplanationTextsUseCase(show)
        }
    }

    fun updateExplanationTextsPromptTiming(
        scope: CoroutineScope,
        timing: ExplanationTextsPromptTiming,
    ) {
        scope.launch {
            updateExplanationTextsPromptTimingUseCase(timing)
        }
    }

    fun resetExplanationTextsPrompt(scope: CoroutineScope) {
        scope.launch {
            resetExplanationTextsPromptUseCase()
        }
    }

    fun markExplanationTextsPromptHandled(scope: CoroutineScope) {
        scope.launch {
            markExplanationTextsPromptHandledUseCase()
        }
    }

    private fun filterPresentedActivities(
        activities: List<ActivityCardUiModel>,
        searchQuery: String,
        dateRangeFilter: ActivityDateRangeFilter,
        sortOption: ActivitySortOption,
    ): List<ActivityCardUiModel> = filterAndSortActivities(
        activities = activities,
        searchQuery = searchQuery,
        dateRangeFilter = dateRangeFilter,
        sortOption = sortOption,
    )

    private fun s(@StringRes resId: Int, vararg args: Any): String =
        stringResolver.get(resId, args)

    private companion object {
        private const val ACTIVITIES_PAGE_SIZE = 20
    }
}
