package info.meuse24.m24bikestats.di

import androidx.room.Room
import info.meuse24.m24bikestats.auth.AuthManager
import info.meuse24.m24bikestats.auth.LoginRepository
import info.meuse24.m24bikestats.data.local.database.BoschDatabase
import info.meuse24.m24bikestats.data.local.database.BoschDatabaseMigrations
import info.meuse24.m24bikestats.data.remote.BoschApiClient
import info.meuse24.m24bikestats.data.repository.BoschRepositoryImpl
import info.meuse24.m24bikestats.data.repository.BoschSmartSystemRepositoryImpl
import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.domain.repository.BoschRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository
import info.meuse24.m24bikestats.domain.usecase.FetchBoschDataUseCase
import info.meuse24.m24bikestats.domain.usecase.ExportSmartSystemActivitiesCsvUseCase
import info.meuse24.m24bikestats.domain.usecase.GetSmartSystemActivityDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.GetSmartSystemActivitiesUseCase
import info.meuse24.m24bikestats.domain.usecase.GetCachedSmartSystemBikeUseCase
import info.meuse24.m24bikestats.domain.usecase.GetSmartSystemBikeDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.GetSmartSystemBikesUseCase
import info.meuse24.m24bikestats.domain.usecase.GetCachedSmartSystemActivityUseCase
import info.meuse24.m24bikestats.domain.usecase.GetCachedSmartSystemActivityDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemActivityDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemActivitiesUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemBikeDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.ObserveCachedSmartSystemBikesUseCase
import info.meuse24.m24bikestats.domain.usecase.RefreshSmartSystemActivitiesUseCase
import info.meuse24.m24bikestats.domain.usecase.RefreshSmartSystemActivityDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.RefreshSmartSystemBikeDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.RefreshSmartSystemBikesUseCase
import info.meuse24.m24bikestats.presentation.apitest.ApiTestViewModel
import info.meuse24.m24bikestats.presentation.dashboard.DashboardViewModel
import info.meuse24.m24bikestats.presentation.login.LoginViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {

    // --- Data ---
    single { BoschApiClient() }
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
    single<BoschRepository> { BoschRepositoryImpl(get()) }
    single<BoschSmartSystemRepository> { BoschSmartSystemRepositoryImpl(get(), get(), get(), get()) }
    single<AuthRepository> { get<AuthManager>() }
    single<LoginRepository> { get<AuthManager>() }

    // --- Domain ---
    factory { FetchBoschDataUseCase(get(), get()) }
    factory { GetSmartSystemActivitiesUseCase(get(), get()) }
    factory { ObserveCachedSmartSystemActivitiesUseCase(get()) }
    factory { ObserveCachedSmartSystemActivityDetailUseCase(get()) }
    factory { ObserveCachedSmartSystemBikesUseCase(get()) }
    factory { ObserveCachedSmartSystemBikeDetailUseCase(get()) }
    factory { GetCachedSmartSystemActivityUseCase(get()) }
    factory { GetCachedSmartSystemActivityDetailUseCase(get()) }
    factory { GetCachedSmartSystemBikeUseCase(get()) }
    factory { ExportSmartSystemActivitiesCsvUseCase(get(), get()) }
    factory { GetSmartSystemActivityDetailUseCase(get(), get()) }
    factory { GetSmartSystemBikesUseCase(get(), get()) }
    factory { GetSmartSystemBikeDetailUseCase(get(), get()) }
    factory { RefreshSmartSystemActivitiesUseCase(get(), get()) }
    factory { RefreshSmartSystemActivityDetailUseCase(get(), get()) }
    factory { RefreshSmartSystemBikesUseCase(get(), get()) }
    factory { RefreshSmartSystemBikeDetailUseCase(get(), get()) }

    // --- Presentation ---
    viewModelOf(::LoginViewModel)
    viewModelOf(::ApiTestViewModel)
    viewModelOf(::DashboardViewModel)
}
