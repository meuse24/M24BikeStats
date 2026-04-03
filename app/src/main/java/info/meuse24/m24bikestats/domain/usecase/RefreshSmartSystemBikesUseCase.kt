package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.BoschBike
import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository

class RefreshSmartSystemBikesUseCase(
    private val repository: BoschSmartSystemRepository,
    private val authRepository: AuthRepository,
    private val cacheTtlMillis: Long = DEFAULT_CACHE_TTL_MS,
) {
    suspend operator fun invoke(force: Boolean = false): Result<List<BoschBike>?> {
        if (!force && repository.isBikesCacheFresh(cacheTtlMillis)) {
            return Result.success(null)
        }

        return withValidAccessToken(authRepository) { token ->
            repository.getBikes(token)
        }
    }

    private companion object {
        const val DEFAULT_CACHE_TTL_MS = 5 * 60 * 1000L
    }
}
