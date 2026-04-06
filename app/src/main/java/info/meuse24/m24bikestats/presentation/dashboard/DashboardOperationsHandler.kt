package info.meuse24.m24bikestats.presentation.dashboard

import androidx.annotation.StringRes
import info.meuse24.m24bikestats.R
import info.meuse24.m24bikestats.domain.model.CloudSyncDetailMode
import info.meuse24.m24bikestats.domain.model.SmartSystemCloudSyncPhase
import info.meuse24.m24bikestats.domain.repository.PdfReportFileExporter
import info.meuse24.m24bikestats.domain.usecase.ExportPdfSummaryReportUseCase
import info.meuse24.m24bikestats.domain.usecase.ExportSmartSystemActivityDetailsCsvUseCase
import info.meuse24.m24bikestats.domain.usecase.ExportSmartSystemActivitiesCsvUseCase
import info.meuse24.m24bikestats.domain.usecase.SyncSmartSystemCloudUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DashboardOperationsHandler(
    private val exportActivitiesCsv: ExportSmartSystemActivitiesCsvUseCase,
    private val exportActivityDetailsCsv: ExportSmartSystemActivityDetailsCsvUseCase,
    private val exportPdfSummaryReportUseCase: ExportPdfSummaryReportUseCase,
    private val pdfReportFileExporter: PdfReportFileExporter,
    private val syncSmartSystemCloudUseCase: SyncSmartSystemCloudUseCase,
    private val stringResolver: DashboardStringResolver,
) {
    private var activitiesExportJob: Job? = null
    private var activityDetailsExportJob: Job? = null
    private var pdfExportJob: Job? = null
    private var cloudSyncJob: Job? = null

    fun exportAllActivitiesCsv(
        scope: CoroutineScope,
        currentState: () -> DashboardUiState,
        updateState: ((DashboardUiState) -> DashboardUiState) -> Unit,
    ) {
        if (!currentState().canRunBackgroundOperation()) return

        activitiesExportJob = scope.launch {
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
                if (error is CancellationException) {
                    updateState {
                        it.copy(
                            isExportingActivitiesCsv = false,
                            exportLoadedActivityCount = 0,
                            exportTotalActivityCount = 0,
                            error = s(R.string.dashboard_info_export_cancelled),
                        )
                    }
                    activitiesExportJob = null
                    return@launch
                }
                updateState {
                    it.copy(
                        isExportingActivitiesCsv = false,
                        exportLoadedActivityCount = 0,
                        exportTotalActivityCount = 0,
                        error = error.message ?: s(R.string.dashboard_error_csv_export),
                    )
                }
                activitiesExportJob = null
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
            activitiesExportJob = null
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

        activityDetailsExportJob = scope.launch {
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
                if (error is CancellationException) {
                    updateState {
                        it.copy(
                            isExportingActivityDetailsCsv = false,
                            exportDetailedLoadedActivityCount = 0,
                            exportDetailedTotalActivityCount = 0,
                            error = s(R.string.dashboard_info_export_cancelled),
                        )
                    }
                    activityDetailsExportJob = null
                    return@launch
                }
                updateState {
                    it.copy(
                        isExportingActivityDetailsCsv = false,
                        exportDetailedLoadedActivityCount = 0,
                        exportDetailedTotalActivityCount = 0,
                        error = error.message ?: s(R.string.dashboard_error_detail_csv_export),
                    )
                }
                activityDetailsExportJob = null
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
            activityDetailsExportJob = null
        }
    }

    fun exportPdfSummaryReport(
        scope: CoroutineScope,
        currentState: () -> DashboardUiState,
        updateState: ((DashboardUiState) -> DashboardUiState) -> Unit,
    ) {
        if (!currentState().canRunBackgroundOperation()) return

        pdfExportJob = scope.launch {
            updateState {
                it.copy(
                    isExportingPdf = true,
                    pendingPdfExport = null,
                    error = null,
                )
            }

            val reportData = exportPdfSummaryReportUseCase().getOrElse { error ->
                if (error is CancellationException) {
                    updateState {
                        it.copy(
                            isExportingPdf = false,
                            error = s(R.string.dashboard_info_export_cancelled),
                        )
                    }
                    pdfExportJob = null
                    return@launch
                }
                updateState {
                    it.copy(
                        isExportingPdf = false,
                        error = error.message ?: s(R.string.dashboard_error_pdf_export),
                    )
                }
                pdfExportJob = null
                return@launch
            }

            val fileName = buildPdfFileName()
            val file = runCatching {
                pdfReportFileExporter.generate(reportData, fileName)
            }.getOrElse { error ->
                updateState {
                    it.copy(
                        isExportingPdf = false,
                        error = error.message ?: s(R.string.dashboard_error_pdf_export),
                    )
                }
                pdfExportJob = null
                return@launch
            }

            updateState {
                it.copy(
                    isExportingPdf = false,
                    pendingPdfExport = PdfExportUiModel(
                        fileName = fileName,
                        filePath = file.absolutePath,
                    ),
                    lastPdfExport = PdfExportSummaryUiModel(
                        fileName = fileName,
                        exportedAtLabel = LocalDateTime.now().format(EXPORT_DATE_TIME_FORMATTER),
                    ),
                    error = null,
                )
            }
            pdfExportJob = null
        }
    }

    fun cancelActivitiesCsvExport(
        updateState: ((DashboardUiState) -> DashboardUiState) -> Unit,
    ) {
        activitiesExportJob?.cancel()
        activitiesExportJob = null
        updateState {
            it.copy(
                isExportingActivitiesCsv = false,
                exportLoadedActivityCount = 0,
                exportTotalActivityCount = 0,
                pendingActivitiesCsvExport = null,
                error = s(R.string.dashboard_info_export_cancelled),
            )
        }
    }

    fun cancelActivityDetailsCsvExport(
        updateState: ((DashboardUiState) -> DashboardUiState) -> Unit,
    ) {
        activityDetailsExportJob?.cancel()
        activityDetailsExportJob = null
        updateState {
            it.copy(
                isExportingActivityDetailsCsv = false,
                exportDetailedLoadedActivityCount = 0,
                exportDetailedTotalActivityCount = 0,
                pendingActivityDetailsCsvExport = null,
                error = s(R.string.dashboard_info_export_cancelled),
            )
        }
    }

    fun cancelPdfExport(
        updateState: ((DashboardUiState) -> DashboardUiState) -> Unit,
    ) {
        pdfExportJob?.cancel()
        pdfExportJob = null
        updateState {
            it.copy(
                isExportingPdf = false,
                pendingPdfExport = null,
                error = s(R.string.dashboard_info_export_cancelled),
            )
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

    fun onPdfExportHandled(
        updateState: ((DashboardUiState) -> DashboardUiState) -> Unit,
    ) {
        updateState {
            it.copy(
                pendingPdfExport = null,
            )
        }
    }

    fun syncCloudData(
        scope: CoroutineScope,
        currentState: () -> DashboardUiState,
        updateState: ((DashboardUiState) -> DashboardUiState) -> Unit,
    ) {
        if (!currentState().canRunBackgroundOperation()) return
        val detailMode = currentState().cloudSyncDetailMode

        cloudSyncJob = scope.launch {
            updateState {
                it.copy(
                    isSyncingCloudData = true,
                    syncPhase = null,
                    syncPhaseLabel = null,
                    syncLoadedActivityCount = 0,
                    syncTotalActivityCount = 0,
                    error = null,
                )
            }

            val summary = syncSmartSystemCloudUseCase(detailMode = detailMode) { progress ->
                updateState {
                    it.copy(
                        syncPhase = progress.phase,
                        syncPhaseLabel = syncPhaseLabel(progress.phase, detailMode),
                        syncLoadedActivityCount = progress.processedCount,
                        syncTotalActivityCount = progress.totalCount,
                    )
                }
            }.getOrElse { error ->
                if (error is CancellationException) {
                    updateState {
                        it.copy(
                            isSyncingCloudData = false,
                            syncPhase = null,
                            syncPhaseLabel = null,
                            syncLoadedActivityCount = 0,
                            syncTotalActivityCount = 0,
                            error = s(R.string.dashboard_info_sync_cancelled),
                        )
                    }
                    cloudSyncJob = null
                    return@launch
                }
                updateState {
                    it.copy(
                        isSyncingCloudData = false,
                        syncPhase = null,
                        syncPhaseLabel = null,
                        syncLoadedActivityCount = 0,
                        syncTotalActivityCount = 0,
                        error = error.message ?: s(R.string.dashboard_error_cloud_sync),
                    )
                }
                cloudSyncJob = null
                return@launch
            }

            updateState {
                it.copy(
                    isSyncingCloudData = false,
                    syncPhase = null,
                    syncPhaseLabel = null,
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
            cloudSyncJob = null
        }
    }

    fun cancelCloudSync(
        updateState: ((DashboardUiState) -> DashboardUiState) -> Unit,
    ) {
        cloudSyncJob?.cancel()
        cloudSyncJob = null
        updateState {
            it.copy(
                isSyncingCloudData = false,
                syncPhase = null,
                syncPhaseLabel = null,
                syncLoadedActivityCount = 0,
                syncTotalActivityCount = 0,
                error = s(R.string.dashboard_info_sync_cancelled),
            )
        }
    }

    private fun DashboardUiState.canRunBackgroundOperation(): Boolean =
        !isInitialLoading &&
            !isRefreshing &&
            !isSyncingCloudData &&
            !isExportingActivitiesCsv &&
            !isExportingActivityDetailsCsv &&
            !isExportingPdf

    private fun s(@StringRes resId: Int, vararg args: Any): String =
        stringResolver.get(resId, args)

    private fun syncPhaseLabel(
        phase: SmartSystemCloudSyncPhase,
        detailMode: CloudSyncDetailMode,
    ): String = when (phase) {
        SmartSystemCloudSyncPhase.BIKES -> s(R.string.home_sync_phase_bikes)
        SmartSystemCloudSyncPhase.ACTIVITIES -> s(R.string.home_sync_phase_activities)
        SmartSystemCloudSyncPhase.ACTIVITY_DETAILS -> when (detailMode) {
            CloudSyncDetailMode.MISSING_ONLY -> s(R.string.home_sync_phase_activity_details_missing_only)
            CloudSyncDetailMode.MISSING_OR_STALE -> s(R.string.home_sync_phase_activity_details_missing_or_stale)
        }
    }

    private companion object {
        private val EXPORT_DATE_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

        fun buildPdfFileName(): String =
            "m24-bericht-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}.pdf"
    }
}
