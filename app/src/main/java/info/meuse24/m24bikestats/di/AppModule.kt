package info.meuse24.m24bikestats.di

import info.meuse24.m24bikestats.auth.AuthManager
import info.meuse24.m24bikestats.auth.LoginRepository
import info.meuse24.m24bikestats.data.remote.BoschApiClient
import info.meuse24.m24bikestats.data.repository.BoschRepositoryImpl
import info.meuse24.m24bikestats.data.repository.BoschSmartSystemRepositoryImpl
import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.domain.repository.BoschRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository
import info.meuse24.m24bikestats.domain.usecase.FetchBoschDataUseCase
import info.meuse24.m24bikestats.domain.usecase.GetSmartSystemActivityDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.GetSmartSystemActivitiesUseCase
import info.meuse24.m24bikestats.domain.usecase.GetSmartSystemBikeDetailUseCase
import info.meuse24.m24bikestats.domain.usecase.GetSmartSystemBikesUseCase
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
    single<BoschRepository> { BoschRepositoryImpl(get()) }
    single<BoschSmartSystemRepository> { BoschSmartSystemRepositoryImpl(get()) }
    single<AuthRepository> { get<AuthManager>() }
    single<LoginRepository> { get<AuthManager>() }

    // --- Domain ---
    factory { FetchBoschDataUseCase(get(), get()) }
    factory { GetSmartSystemActivitiesUseCase(get(), get()) }
    factory { GetSmartSystemActivityDetailUseCase(get(), get()) }
    factory { GetSmartSystemBikesUseCase(get(), get()) }
    factory { GetSmartSystemBikeDetailUseCase(get(), get()) }

    // --- Presentation ---
    viewModelOf(::LoginViewModel)
    viewModelOf(::ApiTestViewModel)
    viewModelOf(::DashboardViewModel)
}
