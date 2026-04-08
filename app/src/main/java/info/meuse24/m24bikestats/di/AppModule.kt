package info.meuse24.m24bikestats.di

import androidx.room.Room
import info.meuse24.m24bikestats.auth.AuthFlowCoordinator
import info.meuse24.m24bikestats.auth.LiveOidcDiscoveryInfoProvider
import info.meuse24.m24bikestats.auth.LiveOidcCertificateInfoProvider
import info.meuse24.m24bikestats.auth.LiveOidcUserInfoProvider
import info.meuse24.m24bikestats.auth.OidcDiscoveryInfoProvider
import info.meuse24.m24bikestats.auth.OidcCertificateInfoProvider
import info.meuse24.m24bikestats.auth.OidcUserInfoProvider
import info.meuse24.m24bikestats.background.BackgroundSyncScheduler
import info.meuse24.m24bikestats.background.BackgroundSyncSettingsObserver
import info.meuse24.m24bikestats.background.ComputeActivityCentersWorker
import info.meuse24.m24bikestats.data.auth.AuthManager
import info.meuse24.m24bikestats.data.export.AndroidPdfStringResolver
import info.meuse24.m24bikestats.data.export.PdfMapTileProvider
import info.meuse24.m24bikestats.data.export.PdfReportGenerator
import info.meuse24.m24bikestats.data.export.PdfReportMetadataRepositoryImpl
import info.meuse24.m24bikestats.data.export.PdfStringResolver
import info.meuse24.m24bikestats.data.local.database.BoschDatabase
import info.meuse24.m24bikestats.data.local.database.BoschDatabaseMigrations
import info.meuse24.m24bikestats.data.local.preferences.AppSettingsRepositoryImpl
import info.meuse24.m24bikestats.data.remote.BoschApiDataSource
import info.meuse24.m24bikestats.data.remote.BoschApiClient
import info.meuse24.m24bikestats.data.remote.BoschJsonBodyExtractor
import info.meuse24.m24bikestats.data.remote.BoschSmartSystemParser
import info.meuse24.m24bikestats.data.repository.BoschRepositoryImpl
import info.meuse24.m24bikestats.data.repository.BoschSmartSystemRepositoryImpl
import info.meuse24.m24bikestats.domain.repository.AppSettingsRepository
import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.domain.repository.BoschApiRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemCacheStatusRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository
import info.meuse24.m24bikestats.domain.repository.PdfReportFileExporter
import info.meuse24.m24bikestats.domain.repository.PdfReportMetadataRepository
import info.meuse24.m24bikestats.domain.usecase.ClearAuthenticationUseCase
import info.meuse24.m24bikestats.domain.usecase.ExportPdfSummaryReportFileUseCase
import info.meuse24.m24bikestats.domain.usecase.ExportPdfSummaryReportUseCase
import info.meuse24.m24bikestats.domain.usecase.ExportSmartSystemActivitiesCsvUseCase
import info.meuse24.m24bikestats.domain.usecase.ExportSmartSystemActivityDetailsCsvUseCase
import info.meuse24.m24bikestats.domain.usecase.FetchBoschDataUseCase
import info.meuse24.m24bikestats.domain.usecase.GetActivityMapPointsUseCase
import info.meuse24.m24bikestats.domain.usecase.GetCachedSmartSystemActivityUseCase
import info.meuse24.m24bikestats.domain.usecase.GetCachedSmartSystemActivityDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.GetCachedSmartSystemActivityTotalCountUseCase
import info.meuse24.m24bikestats.domain.usecase.GetCachedSmartSystemBikeUseCase
import info.meuse24.m24bikestats.domain.usecase.GetCurrentAccessTokenInfoUseCase
import info.meuse24.m24bikestats.domain.usecase.GetStatisticsUseCase
import info.meuse24.m24bikestats.domain.usecase.IsAuthenticatedUseCase
import info.meuse24.m24bikestats.domain.usecase.GetSmartSystemActivitiesUseCase
import info.meuse24.m24bikestats.domain.usecase.GetSmartSystemActivityDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.GetSmartSystemBikeDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.GetSmartSystemBikesUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveAppSettingsUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemActivityDetailCacheOverviewUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemActivityDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemActivitiesUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemBikeDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemBikesUseCase
import info.meuse24.m24bikestats.domain.usecase.RefreshSmartSystemActivitiesUseCase
import info.meuse24.m24bikestats.domain.usecase.RefreshSmartSystemActivityDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.RefreshSmartSystemBikeDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.RefreshSmartSystemBikesUseCase
import info.meuse24.m24bikestats.domain.usecase.SyncSmartSystemCloudUseCase
import info.meuse24.m24bikestats.domain.usecase.UpdateBackgroundSyncModeUseCase
import info.meuse24.m24bikestats.domain.usecase.UpdateCloudSyncDetailModeUseCase
import info.meuse24.m24bikestats.domain.usecase.UpdateCsvExportFormatUseCase
import info.meuse24.m24bikestats.domain.usecase.UpdateDisplayModeUseCase
import info.meuse24.m24bikestats.presentation.dashboard.AndroidDashboardStringResolver
import info.meuse24.m24bikestats.presentation.dashboard.DashboardDetailActionHandler
import info.meuse24.m24bikestats.presentation.dashboard.DashboardFeedHandler
import info.meuse24.m24bikestats.presentation.dashboard.DashboardOperationsHandler
import info.meuse24.m24bikestats.presentation.dashboard.DashboardStringResolver
import info.meuse24.m24bikestats.presentation.dashboard.DashboardUiModelMapper
import info.meuse24.m24bikestats.presentation.apitest.ApiTestViewModel
import info.meuse24.m24bikestats.presentation.dashboard.DashboardViewModel
import info.meuse24.m24bikestats.presentation.login.AndroidLoginStringResolver
import info.meuse24.m24bikestats.presentation.login.LoginStringResolver
import info.meuse24.m24bikestats.presentation.login.LoginViewModel
import info.meuse24.m24bikestats.presentation.map.MapSummaryViewModel
import info.meuse24.m24bikestats.presentation.statistics.StatisticsUiModelMapper
import info.meuse24.m24bikestats.presentation.statistics.StatisticsViewModel
import androidx.work.WorkManager
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Constraints
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {

    // --- Data ---
    single<BoschApiDataSource> { BoschApiClient() }
    single { BoschSmartSystemParser() }
    single { BoschJsonBodyExtractor() }
    single { AuthManager(androidContext()) }
    single {
        Room.databaseBuilder(
            androidContext(),
            BoschDatabase::class.java,
            "bosch_cache.db",
        )
            .addMigrations(*BoschDatabaseMigrations.ALL)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
    single { get<BoschDatabase>().activityDao() }
    single { get<BoschDatabase>().activityDetailDao() }
    single { get<BoschDatabase>().bikeDao() }
    single<BoschApiRepository> { BoschRepositoryImpl(get()) }
    single {
        BoschSmartSystemRepositoryImpl(
            parser = get(),
            activityDao = get(),
            activityDetailDao = get(),
            bikeDao = get(),
            apiClient = get(),
            jsonBodyExtractor = get(),
        )
    }
    single<BoschSmartSystemRepository> { get<BoschSmartSystemRepositoryImpl>() }
    single<BoschSmartSystemCacheStatusRepository> { get<BoschSmartSystemRepositoryImpl>() }
    single<AppSettingsRepository> { AppSettingsRepositoryImpl(androidContext()) }
    single<AuthRepository> { get<AuthManager>() }
    single<AuthFlowCoordinator> { get<AuthManager>() }
    single<LoginStringResolver> { AndroidLoginStringResolver(androidContext()) }
    single { BackgroundSyncScheduler(androidContext()) }
    single { BackgroundSyncSettingsObserver(get(), get()) }
    single<DashboardStringResolver> { AndroidDashboardStringResolver(androidContext()) }
    single { DashboardUiModelMapper(get()) }
    single { StatisticsUiModelMapper() }
    single<OidcCertificateInfoProvider> { LiveOidcCertificateInfoProvider(get(), get()) }
    single<OidcUserInfoProvider> { LiveOidcUserInfoProvider(get()) }
    single<OidcDiscoveryInfoProvider> { LiveOidcDiscoveryInfoProvider(get()) }
    single<PdfReportMetadataRepository> { PdfReportMetadataRepositoryImpl(get(), get()) }
    single<PdfStringResolver> { AndroidPdfStringResolver(androidContext()) }
    single { PdfMapTileProvider() }
    single<PdfReportFileExporter> {
        PdfReportGenerator(
            context = androidContext(),
            stringResolver = get(),
            appVersion = info.meuse24.m24bikestats.BuildConfig.VERSION_NAME,
            mapTileProvider = get(),
        )
    }

    // --- Domain ---
    factory { GetActivityMapPointsUseCase(get()) }
    factory { IsAuthenticatedUseCase(get()) }
    factory { ClearAuthenticationUseCase(get()) }
    factory { FetchBoschDataUseCase(get(), get()) }
    factory { GetSmartSystemActivitiesUseCase(get(), get()) }
    factory { ObserveCachedSmartSystemActivitiesUseCase(get()) }
    factory { ObserveCachedSmartSystemActivityDetailCacheOverviewUseCase(get()) }
    factory { ObserveCachedSmartSystemActivityDetailUseCase(get()) }
    factory { ObserveCachedSmartSystemBikesUseCase(get()) }
    factory { ObserveCachedSmartSystemBikeDetailUseCase(get()) }
    factory { GetCachedSmartSystemActivityUseCase(get()) }
    factory { GetCachedSmartSystemActivityDetailUseCase(get()) }
    factory { GetCachedSmartSystemActivityTotalCountUseCase(get()) }
    factory { GetCachedSmartSystemBikeUseCase(get()) }
    factory { GetStatisticsUseCase(get()) }
    factory { ObserveAppSettingsUseCase(get()) }
    factory { UpdateBackgroundSyncModeUseCase(get()) }
    factory { UpdateCloudSyncDetailModeUseCase(get()) }
    factory { UpdateCsvExportFormatUseCase(get()) }
    factory { UpdateDisplayModeUseCase(get()) }
    factory { ExportSmartSystemActivitiesCsvUseCase(get(), get(), get()) }
    factory { ExportSmartSystemActivityDetailsCsvUseCase(get(), get(), get()) }
    factory { ExportPdfSummaryReportUseCase(get(), get()) }
    factory { ExportPdfSummaryReportFileUseCase(get(), get()) }
    factory { GetCurrentAccessTokenInfoUseCase(get()) }
    factory { GetSmartSystemActivityDetailUseCase(get(), get()) }
    factory { GetSmartSystemBikesUseCase(get(), get()) }
    factory { GetSmartSystemBikeDetailUseCase(get(), get()) }
    factory { RefreshSmartSystemActivitiesUseCase(get(), get(), get()) }
    factory { RefreshSmartSystemActivityDetailUseCase(get(), get(), get()) }
    factory { RefreshSmartSystemBikesUseCase(get(), get(), get()) }
    factory { RefreshSmartSystemBikeDetailUseCase(get(), get(), get()) }
    factory { SyncSmartSystemCloudUseCase(get(), get(), get()) }

    // --- Presentation ---
    factory {
        DashboardFeedHandler(
            observeCachedActivities = get(),
            observeCachedBikes = get(),
            observeCachedActivityDetailCacheOverview = get(),
            observeAppSettings = get(),
            getCachedActivityTotalCount = get(),
            getActivities = get(),
            refreshActivitiesUseCase = get(),
            refreshBikesUseCase = get(),
            updateCloudSyncDetailModeUseCase = get(),
            updateBackgroundSyncModeUseCase = get(),
            updateCsvExportFormatUseCase = get(),
            updateDisplayModeUseCase = get(),
            oidcCertificateInfoProvider = get(),
            uiModelMapper = get(),
            stringResolver = get(),
        )
    }
    factory {
        DashboardOperationsHandler(
            exportActivitiesCsv = get(),
            exportActivityDetailsCsv = get(),
            exportPdfSummaryReportFileUseCase = get(),
            syncSmartSystemCloudUseCase = get(),
            stringResolver = get(),
        )
    }
    factory {
        DashboardDetailActionHandler(
            observeCachedActivityDetail = get(),
            observeCachedBikeDetail = get(),
            getCachedActivity = get(),
            getCachedActivityDetail = get(),
            getCachedBike = get(),
            refreshActivityDetailUseCase = get(),
            refreshBikeDetailUseCase = get(),
            oidcCertificateInfoProvider = get(),
            oidcUserInfoProvider = get(),
            oidcDiscoveryInfoProvider = get(),
            uiModelMapper = get(),
            stringResolver = get(),
        )
    }
    viewModelOf(::LoginViewModel)
    viewModelOf(::ApiTestViewModel)
    viewModelOf(::DashboardViewModel)
    viewModelOf(::StatisticsViewModel)
    viewModelOf(::MapSummaryViewModel)
}
