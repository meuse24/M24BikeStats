package info.meuse24.m24bikestats.presentation.dashboard

import androidx.annotation.StringRes
import info.meuse24.m24bikestats.R
import info.meuse24.m24bikestats.domain.usecase.ExportSmartSystemActivityDetailsCsvUseCase
import info.meuse24.m24bikestats.domain.usecase.ExportSmartSystemActivitiesCsvUseCase
import info.meuse24.m24bikestats.domain.usecase.SyncSmartSystemCloudUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DashboardOperationsHandler(
    private val exportActivitiesCsv: ExportSmartSystemActivitiesCsvUseCase,
    private val exportActivityDetailsCsv: ExportSmartSystemActivityDetailsCsvUseCase,
    private val syncSmartSystemCloudUseCase: SyncSmartSystemCloudUseCase,
    private val stringResolver: DashboardStringResolver,
) {
    fun exportAllActivitiesCsv(
        scope: CoroutineScope,
        currentState: () -> DashboardUiState,
        updateState: ((DashboardUiState) -> DashboardUiState) -> Unit,
    ) {
        if (!currentState().canRunBackgroundOperation()) return

        scope.launch {
            updateState {
                it.copy(
                    isExportingActivitiesCsv = true,
                    exportLoadedActivityCount = 0,
                    exportTotalActivityCount = 0,
                    pendingActivitiesCsvExport = null,
                    pendingActivityDetailsCsvExport = null,
                    error = null,
                )
            }

            val export = exportActivitiesCsv { loadedCount, totalCount ->
                updateState {
                    it.copy(
                        exportLoadedActivityCount = loadedCount,
                        exportTotalActivityCount = totalCount,
                    )
                }
            }.getOrElse { error ->
                updateState {
                    it.copy(
                        isExportingActivitiesCsv = false,
                        exportLoadedActivityCount = 0,
                        exportTotalActivityCount = 0,
                        error = error.message ?: s(R.string.dashboard_error_csv_export),
                    )
                }
                return@launch
            }

            updateState {
                it.copy(
                    isExportingActivitiesCsv = false,
                    exportLoadedActivityCount = export.activityCount,
                    exportTotalActivityCount = export.activityCount,
                    pendingActivitiesCsvExport = ActivitiesCsvExportUiModel(
                        fileName = export.fileName,
                        csvContent = export.csvContent,
                        activityCount = export.activityCount,
                    ),
                    lastActivitiesCsvExport = ActivitiesCsvExportSummaryUiModel(
                        fileName = export.fileName,
                        activityCount = export.activityCount,
                        exportedAtLabel = LocalDateTime.now().format(EXPORT_DATE_TIME_FORMATTER),
                    ),
                    error = null,
                )
            }
        }
    }

    fun exportVisibleActivityDetailsCsv(
        scope: CoroutineScope,
        currentState: () -> DashboardUiState,
        updateState: ((DashboardUiState) -> DashboardUiState) -> Unit,
    ) {
        val state = currentState()
        if (!state.canRunBackgroundOperation()) return

        val activityIds = state.activities.map { it.id }.distinct()
        if (activityIds.isEmpty()) {
            updateState { it.copy(error = s(R.string.dashboard_error_no_visible_activities)) }
            return
        }

        scope.launch {
            updateState {
                it.copy(
                    isExportingActivityDetailsCsv = true,
                    exportDetailedLoadedActivityCount = 0,
                    exportDetailedTotalActivityCount = activityIds.size,
                    pendingActivityDetailsCsvExport = null,
                    error = null,
                )
            }

            val export = exportActivityDetailsCsv(activityIds) { processedCount, totalCount ->
                updateState {
                    it.copy(
                        exportDetailedLoadedActivityCount = processedCount,
                        exportDetailedTotalActivityCount = totalCount,
                    )
                }
            }.getOrElse { error ->
                updateState {
                    it.copy(
                        isExportingActivityDetailsCsv = false,
                        exportDetailedLoadedActivityCount = 0,
                        exportDetailedTotalActivityCount = 0,
                        error = error.message ?: s(R.string.dashboard_error_detail_csv_export),
                    )
                }
                return@launch
            }

            updateState {
                it.copy(
                    isExportingActivityDetailsCsv = false,
                    exportDetailedLoadedActivityCount = export.activityCount,
                    exportDetailedTotalActivityCount = export.activityCount,
                    pendingActivityDetailsCsvExport = ActivityDetailsCsvExportUiModel(
                        fileName = export.fileName,
                        csvContent = export.csvContent,
                        activityCount = export.activityCount,
                        detailPointCount = export.detailPointCount,
                    ),
                    lastActivityDetailsCsvExport = ActivityDetailsCsvExportSummaryUiModel(
                        fileName = export.fileName,
                        activityCount = export.activityCount,
                        detailPointCount = export.detailPointCount,
                        exportedAtLabel = LocalDateTime.now().format(EXPORT_DATE_TIME_FORMATTER),
                    ),
                    error = null,
                )
            }
        }
    }

    fun onActivitiesCsvExportHandled(
        updateState: ((DashboardUiState) -> DashboardUiState) -> Unit,
    ) {
        updateState {
            it.copy(
                pendingActivitiesCsvExport = null,
                exportLoadedActivityCount = 0,
                exportTotalActivityCount = 0,
            )
        }
    }

    fun onActivityDetailsCsvExportHandled(
        updateState: ((DashboardUiState) -> DashboardUiState) -> Unit,
    ) {
        updateState {
            it.copy(
                pendingActivityDetailsCsvExport = null,
                exportDetailedLoadedActivityCount = 0,
                exportDetailedTotalActivityCount = 0,
            )
        }
    }

    fun syncCloudData(
        scope: CoroutineScope,
        currentState: () -> DashboardUiState,
        updateState: ((DashboardUiState) -> DashboardUiState) -> Unit,
    ) {
        if (!currentState().canRunBackgroundOperation()) return

        scope.launch {
            updateState {
                it.copy(
                    isSyncingCloudData = true,
                    syncLoadedActivityCount = 0,
                    syncTotalActivityCount = 0,
                    error = null,
                )
            }

            val summary = syncSmartSystemCloudUseCase { loadedCount, totalCount ->
                updateState {
                    it.copy(
                        syncLoadedActivityCount = loadedCount,
                        syncTotalActivityCount = totalCount,
                    )
                }
            }.getOrElse { error ->
                updateState {
                    it.copy(
                        isSyncingCloudData = false,
                        syncLoadedActivityCount = 0,
                        syncTotalActivityCount = 0,
                        error = error.message ?: s(R.string.dashboard_error_cloud_sync),
                    )
                }
                return@launch
            }

            updateState {
                it.copy(
                    isSyncingCloudData = false,
                    syncLoadedActivityCount = summary.activityCount,
                    syncTotalActivityCount = summary.activityCount,
                    lastCloudSyncSummary = CloudSyncSummaryUiModel(
                        activityCount = summary.activityCount,
                        bikeCount = summary.bikeCount,
                        syncedAtLabel = LocalDateTime.now().format(EXPORT_DATE_TIME_FORMATTER),
                    ),
                    error = null,
                )
            }
        }
    }

    private fun DashboardUiState.canRunBackgroundOperation(): Boolean =
        !isInitialLoading &&
            !isRefreshing &&
            !isSyncingCloudData &&
            !isExportingActivitiesCsv &&
            !isExportingActivityDetailsCsv

    private fun s(@StringRes resId: Int, vararg args: Any): String =
        stringResolver.get(resId, args)

    private companion object {
        private val EXPORT_DATE_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    }
}
