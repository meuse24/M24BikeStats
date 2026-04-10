package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.auth.OidcCacheRepository
import info.meuse24.m24bikestats.data.local.database.BoschDatabase
import info.meuse24.m24bikestats.domain.repository.AppSettingsRepository
import info.meuse24.m24bikestats.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ClearAuthenticationUseCase(
    private val authRepository: AuthRepository,
    private val oidcCacheRepository: OidcCacheRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val database: BoschDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend operator fun invoke() {
        authRepository.clearTokens()
        oidcCacheRepository.clearOidcCache()
        appSettingsRepository.resetInitialSyncFlag()
        appSettingsRepository.resetLatestCachedActivityStartTime()
        // Room wird beim Logout ebenfalls geleert: Die App ist an einen einzelnen Bosch-Account
        // gebunden. Ohne diesen Schritt blieben Aktivitäts- und Bike-Daten des abgemeldeten
        // Nutzers im Cache und wären nach einem erneuten Login (ggf. eines anderen Nutzers auf
        // demselben Gerät) noch lesbar — auch im PDF-Export. Das ist ein Datenschutz-Problem.
        // Entscheidung: Room beim Logout immer leeren; der nächste Initial-Sync befüllt ihn neu.
        withContext(ioDispatcher) {
            database.clearAllTables()
        }
    }
}
