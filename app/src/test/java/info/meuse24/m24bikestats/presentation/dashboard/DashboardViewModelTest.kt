package info.meuse24.m24bikestats.presentation.dashboard

import info.meuse24.m24bikestats.auth.OidcCertificateInfoProvider
import info.meuse24.m24bikestats.auth.OidcCertificateInfoUiModel
import info.meuse24.m24bikestats.auth.OidcDiscoveryInfoProvider
import info.meuse24.m24bikestats.auth.OidcUserInfoProvider
import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.model.ActivityDetailCacheOverview
import info.meuse24.m24bikestats.domain.model.BackgroundSyncMode
import info.meuse24.m24bikestats.domain.model.BoschActivityDetail
import info.meuse24.m24bikestats.domain.model.BoschActivityPage
import info.meuse24.m24bikestats.domain.model.BoschBike
import info.meuse24.m24bikestats.domain.model.CloudSyncDetailMode
import info.meuse24.m24bikestats.domain.model.CsvExportFormat
import info.meuse24.m24bikestats.domain.model.DisplayMode
import info.meuse24.m24bikestats.domain.model.ExplanationTextsPromptTiming
import info.meuse24.m24bikestats.domain.model.PdfReportData
import info.meuse24.m24bikestats.domain.model.PdfReportDiscoveryInfo
import info.meuse24.m24bikestats.domain.model.PdfReportUserInfo
import info.meuse24.m24bikestats.domain.usecase.ExportSmartSystemActivityDetailsCsvUseCase
import info.meuse24.m24bikestats.domain.usecase.ExportPdfSummaryReportFileUseCase
import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemCacheStatusRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository
import info.meuse24.m24bikestats.domain.repository.PdfReportFileExporter
import info.meuse24.m24bikestats.domain.repository.PdfReportMetadataRepository
import info.meuse24.m24bikestats.domain.usecase.ExportPdfSummaryReportUseCase
import info.meuse24.m24bikestats.domain.usecase.ExportSmartSystemActivitiesCsvUseCase
import info.meuse24.m24bikestats.domain.usecase.FakeAppSettingsRepository
import info.meuse24.m24bikestats.domain.usecase.GetCachedSmartSystemActivityDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.GetCachedSmartSystemActivityUseCase
import info.meuse24.m24bikestats.domain.usecase.GetCachedSmartSystemActivityTotalCountUseCase
import info.meuse24.m24bikestats.domain.usecase.GetCachedSmartSystemBikeUseCase
import info.meuse24.m24bikestats.domain.usecase.GetSmartSystemActivitiesUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemActivitiesUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemActivityDetailCacheOverviewUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemActivityDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemBikeDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemBikesUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveAppSettingsUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveDataStatusOverviewUseCase
import info.meuse24.m24bikestats.domain.usecase.MarkExplanationTextsPromptHandledUseCase
import info.meuse24.m24bikestats.domain.usecase.RefreshPendingSmartSystemActivityDetailsUseCase
import info.meuse24.m24bikestats.domain.usecase.RefreshSmartSystemActivitiesUseCase
import info.meuse24.m24bikestats.domain.usecase.RefreshSmartSystemActivityDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.RefreshSmartSystemBikeDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.RefreshSmartSystemBikesUseCase
import info.meuse24.m24bikestats.domain.usecase.ResetExplanationTextsPromptUseCase
import info.meuse24.m24bikestats.domain.usecase.SyncSmartSystemCloudUseCase
import info.meuse24.m24bikestats.domain.usecase.UpdateCloudSyncDetailModeUseCase
import info.meuse24.m24bikestats.domain.usecase.UpdateBackgroundSyncModeUseCase
import info.meuse24.m24bikestats.domain.usecase.UpdateCsvExportFormatUseCase
import info.meuse24.m24bikestats.domain.usecase.UpdateDisplayModeUseCase
import info.meuse24.m24bikestats.domain.usecase.UpdateExplanationTextsPromptTimingUseCase
import info.meuse24.m24bikestats.domain.usecase.UpdateShowExplanationTextsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `search query filters visible activities in ui state`() = runTest {
        val repository = DashboardFakeRepository().apply {
            setActivities(
                listOf(
                    testActivity(id = "a1", title = "Morgenrunde", startTime = "2026-04-02T08:00:00Z"),
                    testActivity(id = "a2", title = "Pendelfahrt", startTime = "2026-04-01T08:00:00Z"),
                ),
                totalCount = 2,
            )
            setBikes(emptyList())
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.updateActivitySearchQuery("morgen")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("morgen", state.activitySearchQuery)
        assertEquals(1, state.visibleActivityCount)
        assertEquals(listOf("a1"), state.activities.map { it.id })
    }

    @Test
    fun `sort option reorders visible activities`() = runTest {
        val repository = DashboardFakeRepository().apply {
            setActivities(
                listOf(
                    testActivity(id = "short", title = "Kurz", distanceMeters = 1000),
                    testActivity(id = "long", title = "Lang", distanceMeters = 9000),
                    testActivity(id = "mid", title = "Mittel", distanceMeters = 4000),
                ),
                totalCount = 3,
            )
            setBikes(emptyList())
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.updateActivitySortOption(ActivitySortOption.LONGEST_DISTANCE)
        advanceUntilIdle()

        assertEquals(listOf("long", "mid", "short"), viewModel.uiState.value.activities.map { it.id })
    }

    @Test
    fun `csv export stores last export summary`() = runTest {
        val repository = DashboardFakeRepository().apply {
            setActivities(
                listOf(testActivity(id = "a1", title = "Tour")),
                totalCount = 1,
            )
            setBikes(emptyList())
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.exportAllActivitiesCsv()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.pendingActivitiesCsvExport)
        assertNotNull(state.lastActivitiesCsvExport)
        assertEquals(1, state.lastActivitiesCsvExport?.activityCount)
        assertTrue(state.lastActivitiesCsvExport?.fileName?.endsWith(".csv") == true)
    }

    @Test
    fun `detail csv export stores visible activity export summary`() = runTest {
        val repository = DashboardFakeRepository().apply {
            setActivities(
                listOf(
                    testActivity(id = "a1", title = "Morgenrunde"),
                    testActivity(id = "a2", title = "Abendrunde"),
                ),
                totalCount = 2,
            )
            setBikes(emptyList())
            setActivityDetail(
                "a1",
                BoschActivityDetail(
                    activityId = "a1",
                    points = listOf(
                        info.meuse24.m24bikestats.domain.model.BoschActivityDetailPoint(
                            distanceMeters = 100.0,
                            altitudeMeters = 500.0,
                            speedKmh = 25.0,
                            cadenceRpm = 80.0,
                            latitude = 47.1,
                            longitude = 9.1,
                            riderPowerWatts = 210.0,
                        )
                    )
                )
            )
            setActivityDetail(
                "a2",
                BoschActivityDetail(
                    activityId = "a2",
                    points = listOf(
                        info.meuse24.m24bikestats.domain.model.BoschActivityDetailPoint(
                            distanceMeters = 120.0,
                            altitudeMeters = 520.0,
                            speedKmh = 22.0,
                            cadenceRpm = 78.0,
                            latitude = 47.2,
                            longitude = 9.2,
                            riderPowerWatts = 190.0,
                        )
                    )
                )
            )
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.exportVisibleActivityDetailsCsv()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.pendingActivityDetailsCsvExport)
        assertNotNull(state.lastActivityDetailsCsvExport)
        assertEquals(2, state.lastActivityDetailsCsvExport?.activityCount)
        assertEquals(2, state.lastActivityDetailsCsvExport?.detailPointCount)
    }

    @Test
    fun `pdf export stores last export summary`() = runTest {
        val repository = DashboardFakeRepository().apply {
            setActivities(
                listOf(testActivity(id = "a1", title = "Tour")),
                totalCount = 1,
            )
            setBikes(emptyList())
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.exportPdfSummaryReport()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.pendingPdfExport)
        assertNotNull(state.lastPdfExport)
        assertTrue(state.pendingPdfExport?.filePath?.endsWith(".pdf") == true)
        assertTrue(state.lastPdfExport?.fileName?.endsWith(".pdf") == true)
    }

    @Test
    fun `csv export format changes propagate into ui state`() = runTest {
        val repository = DashboardFakeRepository().apply {
            setActivities(emptyList(), totalCount = 0)
            setBikes(emptyList())
        }
        val settingsRepository = FakeAppSettingsRepository(CsvExportFormat.STANDARD_INTERNATIONAL)
        val viewModel = createViewModel(repository, settingsRepository)
        advanceUntilIdle()

        viewModel.updateCsvExportFormat(CsvExportFormat.EXCEL_DE)
        advanceUntilIdle()

        assertEquals(CsvExportFormat.EXCEL_DE, viewModel.uiState.value.csvExportFormat)
    }

    @Test
    fun `home overview exposes cached detail and gps counters`() = runTest {
        val repository = DashboardFakeRepository().apply {
            setActivities(listOf(testActivity(id = "a1", title = "Tour")), totalCount = 1)
            setBikes(emptyList())
            setActivityDetail(
                "a1",
                BoschActivityDetail(
                    activityId = "a1",
                    points = listOf(
                        info.meuse24.m24bikestats.domain.model.BoschActivityDetailPoint(
                            distanceMeters = 100.0,
                            altitudeMeters = 500.0,
                            speedKmh = 25.0,
                            cadenceRpm = 80.0,
                            latitude = 47.1,
                            longitude = 9.1,
                            riderPowerWatts = 210.0,
                        ),
                        info.meuse24.m24bikestats.domain.model.BoschActivityDetailPoint(
                            distanceMeters = 120.0,
                            altitudeMeters = 505.0,
                            speedKmh = 22.0,
                            cadenceRpm = 78.0,
                            latitude = null,
                            longitude = null,
                            riderPowerWatts = 190.0,
                        ),
                    ),
                )
            )
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        val homeState = viewModel.uiState.value.toHomeUiState()
        assertEquals(1, homeState.cachedDetailActivityCount)
        assertEquals(2, homeState.cachedDetailPointCount)
        assertEquals(1, homeState.cachedGpsPointCount)
    }

    @Test
    fun `home sync availability uses shared background operation guard`() {
        val idleState = DashboardUiState()
        val blockedState = DashboardUiState(isExportingPdf = true)

        assertEquals(idleState.canRunBackgroundOperation(), idleState.toHomeUiState().canStartSync)
        assertEquals(blockedState.canRunBackgroundOperation(), blockedState.toHomeUiState().canStartSync)
        assertEquals(false, blockedState.toHomeUiState().canStartSync)
    }

    @Test
    fun `home data status exposes missing and stale cache state`() = runTest {
        val repository = DashboardFakeRepository().apply {
            setActivities(
                listOf(
                    testActivity(id = "a1", title = "Ride 1", startTime = "2026-04-01T08:00:00Z"),
                    testActivity(id = "a2", title = "Ride 2", startTime = "2026-04-02T08:00:00Z"),
                    testActivity(id = "a3", title = "Ride 3", startTime = "2026-04-03T08:00:00Z"),
                ),
                totalCount = 3,
            )
            setBikes(emptyList())
            setActivityDetail("a1", BoschActivityDetail(activityId = "a1", points = emptyList()))
            setStaleActivityIds(setOf("a1"))
            setCacheTimestamps(activityUpdatedAt = 10L, bikeUpdatedAt = 20L, detailUpdatedAt = 30L)
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        val dataStatus = viewModel.uiState.value.toHomeUiState().dataStatus
        assertNotNull(dataStatus)
        assertEquals(3, dataStatus!!.cachedActivityCount)
        assertEquals(1, dataStatus.detailedActivityCount)
        assertEquals(2, dataStatus.missingDetailCount)
        assertEquals(1, dataStatus.staleDetailCount)
        assertEquals(DataStatusTone.PARTIAL, dataStatus.statusTone)
    }

    @Test
    fun `loading missing activity details updates data status`() = runTest {
        val repository = DashboardFakeRepository().apply {
            setActivities(
                listOf(
                    testActivity(id = "a1", title = "Ride 1"),
                    testActivity(id = "a2", title = "Ride 2"),
                ),
                totalCount = 2,
            )
            setBikes(emptyList())
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.toHomeUiState().dataStatus?.missingDetailCount)

        viewModel.loadMissingActivityDetails()
        advanceUntilIdle()

        val dataStatus = viewModel.uiState.value.toHomeUiState().dataStatus
        assertEquals(0, dataStatus?.missingDetailCount)
        assertEquals(2, dataStatus?.detailedActivityCount)
    }

    @Test
    fun `cloud sync detail mode changes propagate into ui state`() = runTest {
        val repository = DashboardFakeRepository().apply {
            setActivities(emptyList(), totalCount = 0)
            setBikes(emptyList())
        }
        val settingsRepository = FakeAppSettingsRepository()
        val viewModel = createViewModel(repository, settingsRepository)
        advanceUntilIdle()

        viewModel.updateCloudSyncDetailMode(CloudSyncDetailMode.MISSING_OR_STALE)
        advanceUntilIdle()

        assertEquals(CloudSyncDetailMode.MISSING_OR_STALE, viewModel.uiState.value.cloudSyncDetailMode)
    }

    @Test
    fun `background sync mode changes propagate into ui state`() = runTest {
        val repository = DashboardFakeRepository().apply {
            setActivities(emptyList(), totalCount = 0)
            setBikes(emptyList())
        }
        val settingsRepository = FakeAppSettingsRepository()
        val viewModel = createViewModel(repository, settingsRepository)
        advanceUntilIdle()

        viewModel.updateBackgroundSyncMode(BackgroundSyncMode.DAILY_UNMETERED)
        advanceUntilIdle()

        assertEquals(BackgroundSyncMode.DAILY_UNMETERED, viewModel.uiState.value.backgroundSyncMode)
    }

    @Test
    fun `display mode changes propagate into ui state`() = runTest {
        val repository = DashboardFakeRepository().apply {
            setActivities(emptyList(), totalCount = 0)
            setBikes(emptyList())
        }
        val settingsRepository = FakeAppSettingsRepository()
        val viewModel = createViewModel(repository, settingsRepository)
        advanceUntilIdle()

        viewModel.updateDisplayMode(DisplayMode.DARK)
        advanceUntilIdle()

        assertEquals(DisplayMode.DARK, viewModel.uiState.value.displayMode)
    }

    @Test
    fun `show explanation texts changes propagate into ui state`() = runTest {
        val repository = DashboardFakeRepository().apply {
            setActivities(emptyList(), totalCount = 0)
            setBikes(emptyList())
        }
        val settingsRepository = FakeAppSettingsRepository(initialShowExplanationTexts = true)
        val viewModel = createViewModel(repository, settingsRepository)
        advanceUntilIdle()

        viewModel.updateShowExplanationTexts(false)
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.showExplanationTexts)
    }

    @Test
    fun `prompt timing changes propagate into settings repository`() = runTest {
        val repository = DashboardFakeRepository().apply {
            setActivities(emptyList(), totalCount = 0)
            setBikes(emptyList())
        }
        val settingsRepository = FakeAppSettingsRepository()
        val viewModel = createViewModel(repository, settingsRepository)
        advanceUntilIdle()

        viewModel.updateExplanationTextsPromptTiming(ExplanationTextsPromptTiming.LATE)
        advanceUntilIdle()

        assertEquals(
            ExplanationTextsPromptTiming.LATE,
            settingsRepository.getSettings().explanationTextsPromptTiming,
        )
    }

    @Test
    fun `reset explanation prompt clears handled state and usage`() = runTest {
        val repository = DashboardFakeRepository().apply {
            setActivities(emptyList(), totalCount = 0)
            setBikes(emptyList())
        }
        val settingsRepository = FakeAppSettingsRepository(
            initialExplanationTextsPromptForegroundUsageMillis = 5_000L,
            initialExplanationTextsPromptHandled = true,
        )
        val viewModel = createViewModel(repository, settingsRepository)
        advanceUntilIdle()

        viewModel.resetExplanationTextsPrompt()
        advanceUntilIdle()

        val settings = settingsRepository.getSettings()
        assertEquals(0L, settings.explanationTextsPromptForegroundUsageMillis)
        assertEquals(false, settings.explanationTextsPromptHandled)
    }

    @Test
    fun `can load more activities when cached total exceeds first loaded page`() = runTest {
        val repository = DashboardFakeRepository().apply {
            setActivities(
                activities = (1..20).map { index -> testActivity(id = "a$index", title = "Ride $index") },
                totalCount = 45,
            )
            setBikes(emptyList())
        }
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(20, state.loadedActivityCount)
        assertEquals(45, state.activityTotalCount)
        assertTrue(state.canLoadMoreActivities)
    }

    @Test
    fun `cloud sync stores last sync summary`() = runTest {
        val repository = DashboardFakeRepository().apply {
            setActivities(
                listOf(
                    testActivity(id = "a1", title = "Morgenrunde"),
                    testActivity(id = "a2", title = "Abendrunde"),
                ),
                totalCount = 2,
            )
            setBikes(
                listOf(
                    info.meuse24.m24bikestats.domain.model.BoschBike(
                        id = "bike-1",
                        createdAt = null,
                        language = null,
                        driveUnit = null,
                        remoteControl = null,
                        headUnit = null,
                        batteries = emptyList(),
                    )
                )
            )
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.syncCloudData()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.lastCloudSyncSummary)
        assertEquals(2, state.lastCloudSyncSummary?.activityCount)
        assertEquals(1, state.lastCloudSyncSummary?.bikeCount)
        assertEquals(false, state.isSyncingCloudData)
    }

    private fun createViewModel(
        repository: DashboardFakeRepository,
        settingsRepository: FakeAppSettingsRepository = FakeAppSettingsRepository(),
    ): DashboardViewModel {
        val authRepository = object : AuthRepository {
            override fun getAccessToken(): String? = "token"
            override suspend fun getValidAccessToken(): Result<String> = Result.success("token")
            override fun isAuthenticated(): Boolean = true
            override fun clearTokens() = Unit
        }

        return DashboardViewModel(
            feedHandler = DashboardFeedHandler(
                observeCachedActivities = ObserveCachedSmartSystemActivitiesUseCase(repository),
                observeCachedBikes = ObserveCachedSmartSystemBikesUseCase(repository),
                observeCachedActivityDetailCacheOverview = ObserveCachedSmartSystemActivityDetailCacheOverviewUseCase(repository),
                observeDataStatusOverview = ObserveDataStatusOverviewUseCase(repository, repository),
                observeAppSettings = ObserveAppSettingsUseCase(settingsRepository),
                getCachedActivityTotalCount = GetCachedSmartSystemActivityTotalCountUseCase(repository),
                getActivities = GetSmartSystemActivitiesUseCase(repository, authRepository),
                refreshActivitiesUseCase = RefreshSmartSystemActivitiesUseCase(repository, repository, authRepository),
                refreshBikesUseCase = RefreshSmartSystemBikesUseCase(repository, repository, authRepository),
                updateCloudSyncDetailModeUseCase = UpdateCloudSyncDetailModeUseCase(settingsRepository),
                updateBackgroundSyncModeUseCase = UpdateBackgroundSyncModeUseCase(settingsRepository),
                updateCsvExportFormatUseCase = UpdateCsvExportFormatUseCase(settingsRepository),
                updateDisplayModeUseCase = UpdateDisplayModeUseCase(settingsRepository),
                updateExplanationTextsPromptTimingUseCase = UpdateExplanationTextsPromptTimingUseCase(settingsRepository),
                resetExplanationTextsPromptUseCase = ResetExplanationTextsPromptUseCase(settingsRepository),
                markExplanationTextsPromptHandledUseCase = MarkExplanationTextsPromptHandledUseCase(settingsRepository),
                updateShowExplanationTextsUseCase = UpdateShowExplanationTextsUseCase(settingsRepository),
                oidcCertificateInfoProvider = object : OidcCertificateInfoProvider {
                    override suspend fun loadCurrentCertificate(): OidcCertificateInfoUiModel? = null
                },
                uiModelMapper = DashboardUiModelMapper(TestStringResolver()),
                stringResolver = TestStringResolver(),
            ),
            operationsHandler = DashboardOperationsHandler(
                exportActivitiesCsv = ExportSmartSystemActivitiesCsvUseCase(repository, authRepository, settingsRepository),
                exportActivityDetailsCsv = ExportSmartSystemActivityDetailsCsvUseCase(repository, authRepository, settingsRepository),
                exportPdfSummaryReportFileUseCase = ExportPdfSummaryReportFileUseCase(
                    exportPdfSummaryReportUseCase = ExportPdfSummaryReportUseCase(
                        metadataRepository = FakePdfReportMetadataRepository(),
                        repository = repository,
                    ),
                    pdfReportFileExporter = FakePdfReportFileExporter(),
                ),
                refreshPendingSmartSystemActivityDetailsUseCase = RefreshPendingSmartSystemActivityDetailsUseCase(
                    repository = repository,
                    cacheStatusRepository = repository,
                    authRepository = authRepository,
                ),
                syncSmartSystemCloudUseCase = SyncSmartSystemCloudUseCase(repository, repository, authRepository),
                stringResolver = TestStringResolver(),
                pdfExportDispatcher = Dispatchers.Main,
            ),
            detailActionHandler = DashboardDetailActionHandler(
                observeCachedActivityDetail = ObserveCachedSmartSystemActivityDetailUseCase(repository),
                observeCachedBikeDetail = ObserveCachedSmartSystemBikeDetailUseCase(repository),
                getCachedActivity = GetCachedSmartSystemActivityUseCase(repository),
                getCachedActivityDetail = GetCachedSmartSystemActivityDetailUseCase(repository),
                getCachedBike = GetCachedSmartSystemBikeUseCase(repository),
                refreshActivityDetailUseCase = RefreshSmartSystemActivityDetailUseCase(repository, repository, authRepository),
                refreshBikeDetailUseCase = RefreshSmartSystemBikeDetailUseCase(repository, repository, authRepository),
                oidcCertificateInfoProvider = object : OidcCertificateInfoProvider {
                    override suspend fun loadCurrentCertificate(): OidcCertificateInfoUiModel? = null
                },
                oidcUserInfoProvider = object : OidcUserInfoProvider {
                    override suspend fun loadCurrentUserInfo() = null
                },
                oidcDiscoveryInfoProvider = object : OidcDiscoveryInfoProvider {
                    override suspend fun loadCurrentDiscovery() = null
                },
                uiModelMapper = DashboardUiModelMapper(TestStringResolver()),
                stringResolver = TestStringResolver(),
            ),
        )
    }

    private fun testActivity(
        id: String,
        title: String,
        startTime: String = "2026-04-03T10:00:00Z",
        distanceMeters: Int = 1234,
    ) = BoschActivity(
        id = id,
        title = title,
        startTime = startTime,
        endTime = null,
        timeZone = null,
        durationWithoutStopsSeconds = 1200,
        bikeId = null,
        startOdometerMeters = null,
        distanceMeters = distanceMeters,
        averageSpeedKmh = null,
        maxSpeedKmh = null,
        averageCadenceRpm = null,
        maxCadenceRpm = null,
        averageRiderPowerWatts = null,
        maxRiderPowerWatts = null,
        elevationGainMeters = null,
        elevationLossMeters = null,
        caloriesBurned = null,
    )
}

private class FakePdfReportMetadataRepository : PdfReportMetadataRepository {
    override suspend fun getCurrentUserInfo(): Result<PdfReportUserInfo?> =
        Result.success(
            PdfReportUserInfo(
                email = "test@example.com",
                username = "tester",
                subject = "sub-1",
            )
        )

    override suspend fun getCurrentDiscoveryInfo(): Result<PdfReportDiscoveryInfo?> =
        Result.success(
            PdfReportDiscoveryInfo(
                issuer = "https://issuer.example",
                authorizationEndpoint = "https://issuer.example/auth",
                tokenEndpoint = "https://issuer.example/token",
                userInfoEndpoint = "https://issuer.example/userinfo",
            )
        )
}

private class FakePdfReportFileExporter : PdfReportFileExporter {
    override fun generate(
        reportData: PdfReportData,
        fileName: String,
    ): File =
        kotlin.io.path.createTempFile(fileName.removeSuffix(".pdf"), ".pdf").toFile()
}

private class TestStringResolver : DashboardStringResolver {
    override fun get(resId: Int, args: Array<out Any>): String =
        buildString {
            append("res-")
            append(resId)
            if (args.isNotEmpty()) {
                append(':')
                append(args.joinToString(","))
            }
        }
}

private class DashboardFakeRepository :
    BoschSmartSystemRepository,
    BoschSmartSystemCacheStatusRepository {
    private val activitiesFlow = MutableStateFlow<List<BoschActivity>>(emptyList())
    private val bikesFlow = MutableStateFlow<List<BoschBike>>(emptyList())
    private val activityDetails = mutableMapOf<String, MutableStateFlow<BoschActivityDetail?>>()
    private val bikeDetails = mutableMapOf<String, MutableStateFlow<BoschBike?>>()
    private val detailOverviewFlow = MutableStateFlow(currentDetailOverview())
    private val activityCacheUpdatedAtFlow = MutableStateFlow<Long?>(null)
    private val bikeCacheUpdatedAtFlow = MutableStateFlow<Long?>(null)
    private val activityDetailCacheUpdatedAtFlow = MutableStateFlow<Long?>(null)
    private var activityTotalCount: Int? = null
    private var staleActivityIds: Set<String> = emptySet()

    fun setActivities(activities: List<BoschActivity>, totalCount: Int) {
        activitiesFlow.value = activities
        activityTotalCount = totalCount
        detailOverviewFlow.value = currentDetailOverview()
    }

    fun setBikes(bikes: List<BoschBike>) {
        bikesFlow.value = bikes
    }

    fun setActivityDetail(activityId: String, detail: BoschActivityDetail) {
        activityDetails.getOrPut(activityId) { MutableStateFlow(null) }.value = detail
        staleActivityIds = staleActivityIds - activityId
        detailOverviewFlow.value = currentDetailOverview()
    }

    fun setStaleActivityIds(activityIds: Set<String>) {
        staleActivityIds = activityIds
    }

    fun setCacheTimestamps(
        activityUpdatedAt: Long? = activityCacheUpdatedAtFlow.value,
        bikeUpdatedAt: Long? = bikeCacheUpdatedAtFlow.value,
        detailUpdatedAt: Long? = activityDetailCacheUpdatedAtFlow.value,
    ) {
        activityCacheUpdatedAtFlow.value = activityUpdatedAt
        bikeCacheUpdatedAtFlow.value = bikeUpdatedAt
        activityDetailCacheUpdatedAtFlow.value = detailUpdatedAt
    }

    override fun observeCachedActivities(): Flow<List<BoschActivity>> = activitiesFlow.asStateFlow()
    override fun observeCachedBikes(): Flow<List<BoschBike>> = bikesFlow.asStateFlow()
    override fun observeCachedActivityDetailCacheOverview(): Flow<ActivityDetailCacheOverview> =
        detailOverviewFlow.asStateFlow()
    override fun observeActivityCacheUpdatedAtEpochMillis(): Flow<Long?> = activityCacheUpdatedAtFlow.asStateFlow()
    override fun observeBikeCacheUpdatedAtEpochMillis(): Flow<Long?> = bikeCacheUpdatedAtFlow.asStateFlow()
    override fun observeActivityDetailCacheUpdatedAtEpochMillis(): Flow<Long?> = activityDetailCacheUpdatedAtFlow.asStateFlow()
    override fun observeCachedActivityDetail(activityId: String): Flow<BoschActivityDetail?> =
        activityDetails.getOrPut(activityId) { MutableStateFlow(null) }.asStateFlow()

    override fun observeCachedBike(bikeId: String): Flow<BoschBike?> =
        bikeDetails.getOrPut(bikeId) { MutableStateFlow(null) }.asStateFlow()

    override suspend fun getCachedActivities(): List<BoschActivity> = activitiesFlow.value
    override suspend fun getCachedActivityTotalCount(): Int? = activityTotalCount
    override suspend fun getCachedActivity(activityId: String): BoschActivity? =
        activitiesFlow.value.firstOrNull { it.id == activityId }

    override suspend fun getCachedActivityDetail(activityId: String): BoschActivityDetail? =
        activityDetails[activityId]?.value

    override suspend fun getCachedBike(bikeId: String): BoschBike? =
        bikeDetails[bikeId]?.value ?: bikesFlow.value.firstOrNull { it.id == bikeId }

    override suspend fun hasFreshActivities(maxAgeMillis: Long): Boolean = true
    override suspend fun hasFreshActivityDetail(activityId: String, maxAgeMillis: Long): Boolean =
        activityDetails[activityId]?.value != null
    override suspend fun hasFreshBikes(maxAgeMillis: Long): Boolean = true
    override suspend fun hasFreshBikeDetail(bikeId: String, maxAgeMillis: Long): Boolean = true
    override suspend fun getActivityIdsNeedingDetailSync(
        detailMode: CloudSyncDetailMode,
        staleThresholdEpochMillis: Long,
    ): List<String> = activitiesFlow.value.map { it.id }.filter { activityId ->
        val hasDetail = activityDetails[activityId]?.value != null
        when (detailMode) {
            CloudSyncDetailMode.MISSING_ONLY -> !hasDetail
            CloudSyncDetailMode.MISSING_OR_STALE -> !hasDetail || staleActivityIds.contains(activityId)
        }
    }

    override suspend fun getActivities(accessToken: String, limit: Int, offset: Int): Result<BoschActivityPage> =
        Result.success(
            BoschActivityPage(
                total = activityTotalCount ?: activitiesFlow.value.size,
                offset = offset,
                limit = limit,
                items = activitiesFlow.value.drop(offset).take(limit),
            )
        )

    override suspend fun getActivityDetail(accessToken: String, activityId: String): Result<BoschActivityDetail> =
        activityDetails[activityId]?.value?.let { Result.success(it) }
            ?: getCachedActivity(activityId)?.let {
                val detail = BoschActivityDetail(activityId = activityId, points = emptyList())
                activityDetails.getOrPut(activityId) { MutableStateFlow(null) }.value = detail
                staleActivityIds = staleActivityIds - activityId
                detailOverviewFlow.value = currentDetailOverview()
                activityDetailCacheUpdatedAtFlow.value = activityDetailCacheUpdatedAtFlow.value ?: 1L
                Result.success(detail)
            }
            ?: Result.failure(IllegalStateException("not needed"))

    override suspend fun getBikes(accessToken: String): Result<List<BoschBike>> =
        Result.success(bikesFlow.value)

    override suspend fun getBikeDetail(accessToken: String, bikeId: String): Result<BoschBike> =
        Result.failure(IllegalStateException("not needed"))

    private fun currentDetailOverview(): ActivityDetailCacheOverview =
        ActivityDetailCacheOverview(
            detailedActivityCount = activityDetails.values.count { it.value != null },
            detailPointCount = activityDetails.values.sumOf { it.value?.points?.size ?: 0 },
            gpsPointCount = activityDetails.values.sumOf { flow ->
                flow.value?.points?.count { point -> point.latitude != null && point.longitude != null } ?: 0
            },
        )
}
