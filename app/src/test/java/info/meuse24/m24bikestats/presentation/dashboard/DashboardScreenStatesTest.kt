package info.meuse24.m24bikestats.presentation.dashboard

import info.meuse24.m24bikestats.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardScreenStatesTest {

    @Test
    fun `data status derived properties reflect cache completeness`() {
        val complete = dataStatus(
            statusTone = DataStatusTone.COMPLETE,
            cachedActivityCount = 10,
            detailedActivityCount = 10,
            missingDetailCount = 0,
            staleDetailCount = 0,
        )
        val partial = dataStatus(
            statusTone = DataStatusTone.PARTIAL,
            cachedActivityCount = 10,
            detailedActivityCount = 8,
            missingDetailCount = 2,
            staleDetailCount = 1,
        )
        val empty = dataStatus(
            statusTone = DataStatusTone.EMPTY,
            cachedActivityCount = 0,
            detailedActivityCount = 0,
            missingDetailCount = 0,
            staleDetailCount = 0,
        )

        assertTrue(complete.isComplete)
        assertFalse(complete.hasMissingDetails)
        assertFalse(complete.hasStaleDetails)
        assertEquals(100, complete.detailCoveragePercent)
        assertEquals(R.string.home_data_status_headline_ready, complete.statusHeadlineRes)

        assertFalse(partial.isComplete)
        assertTrue(partial.hasMissingDetails)
        assertTrue(partial.hasStaleDetails)
        assertEquals(80, partial.detailCoveragePercent)
        assertEquals(R.string.home_data_status_headline_missing, partial.statusHeadlineRes)

        assertFalse(empty.isComplete)
        assertEquals(0, empty.detailCoveragePercent)
        assertEquals(R.string.home_data_status_headline_empty, empty.statusHeadlineRes)
    }

    @Test
    fun `home ui state derived flags reflect sync metadata and secondary actions`() {
        val state = homeUiState(
            isSyncingCloudData = true,
            dataStatus = dataStatus(
                statusTone = DataStatusTone.PARTIAL,
                missingDetailCount = 3,
                staleDetailCount = 1,
                lastActivitySyncLabel = "09.04.2026 06:10",
            ),
        )

        assertTrue(state.isAnySyncActive)
        assertTrue(state.hasSyncMetadata)
        assertTrue(state.hasSecondarySyncActions)
    }

    @Test
    fun `home ui state status summary visibility follows explanation and cache state`() {
        val completeWithoutHints = homeUiState(
            showExplanationTexts = false,
            dataStatus = dataStatus(
                statusTone = DataStatusTone.COMPLETE,
                missingDetailCount = 0,
                staleDetailCount = 0,
                cachedActivityCount = 12,
                detailedActivityCount = 12,
            ),
        )
        val staleWithoutHints = homeUiState(
            showExplanationTexts = false,
            dataStatus = dataStatus(
                statusTone = DataStatusTone.COMPLETE,
                missingDetailCount = 0,
                staleDetailCount = 2,
                cachedActivityCount = 12,
                detailedActivityCount = 12,
            ),
        )
        val partialWithoutHints = homeUiState(
            showExplanationTexts = false,
            dataStatus = dataStatus(
                statusTone = DataStatusTone.PARTIAL,
                missingDetailCount = 2,
                staleDetailCount = 0,
                cachedActivityCount = 12,
                detailedActivityCount = 10,
            ),
        )
        val noStatus = homeUiState(
            showExplanationTexts = false,
            dataStatus = null,
        )

        assertFalse(completeWithoutHints.shouldShowStatusSummary)
        assertTrue(staleWithoutHints.shouldShowStatusSummary)
        assertTrue(partialWithoutHints.shouldShowStatusSummary)
        assertTrue(noStatus.shouldShowStatusSummary)
    }

    private fun dataStatus(
        statusTone: DataStatusTone,
        cachedActivityCount: Int = 12,
        detailedActivityCount: Int = 12,
        missingDetailCount: Int = 0,
        staleDetailCount: Int = 0,
        lastActivitySyncLabel: String? = null,
    ) = DataStatusUiModel(
        statusTone = statusTone,
        statusLabel = "status",
        statusSummary = "summary",
        coveredPeriodLabel = null,
        cachedActivityCount = cachedActivityCount,
        detailedActivityCount = detailedActivityCount,
        detailCoverageLabel = "coverage",
        missingDetailCount = missingDetailCount,
        staleDetailCount = staleDetailCount,
        gpsPointCount = 0,
        lastActivitySyncLabel = lastActivitySyncLabel,
        lastBikeSyncLabel = null,
        lastDetailSyncLabel = null,
    )

    private fun homeUiState(
        isSyncingCloudData: Boolean = false,
        showExplanationTexts: Boolean = true,
        dataStatus: DataStatusUiModel? = dataStatus(statusTone = DataStatusTone.COMPLETE),
    ) = HomeUiState(
        allActivities = emptyList(),
        bikes = emptyList(),
        loadedActivityCount = 0,
        visibleActivityCount = 0,
        isInitialLoading = false,
        isRefreshing = false,
        isSyncingCloudData = isSyncingCloudData,
        isExportingActivitiesCsv = false,
        isExportingActivityDetailsCsv = false,
        isExportingPdf = false,
        syncPhase = null,
        syncPhaseLabel = null,
        syncLoadedActivityCount = 0,
        syncTotalActivityCount = 0,
        isSyncingPendingActivityDetails = false,
        pendingActivityDetailSyncLabel = null,
        pendingActivityDetailSyncLoadedCount = 0,
        pendingActivityDetailSyncTotalCount = 0,
        cachedDetailActivityCount = 0,
        cachedDetailPointCount = 0,
        cachedGpsPointCount = 0,
        dataStatus = dataStatus,
        canStartSync = true,
        lastCloudSyncSummary = null,
        lastActivitiesCsvExport = null,
        lastActivityDetailsCsvExport = null,
        lastPdfExport = null,
        showExplanationTexts = showExplanationTexts,
    )
}
