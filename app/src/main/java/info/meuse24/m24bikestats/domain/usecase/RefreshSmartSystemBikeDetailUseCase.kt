package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.BoschBike
import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemCacheStatusRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository

class RefreshSmartSystemBikeDetailUseCase(
    private val repository: BoschSmartSystemRepository,
    private val cacheStatusRepository: BoschSmartSystemCacheStatusRepository,
    private val authRepository: AuthRepository,
    private val cacheTtlMillis: Long = DEFAULT_CACHE_TTL_MS,
) {
    suspend operator fun invoke(
        bikeId: String,
        force: Boolean = false,
    ): Result<BoschBike?> {
        if (!force && cacheStatusRepository.hasFreshBikeDetail(bikeId, cacheTtlMillis)) {
            return Result.success(null)
        }

        return withValidAccessToken(authRepository) { token ->
            repository.getBikeDetail(token, bikeId)
        }
    }

    private companion object {
        const val DEFAULT_CACHE_TTL_MS = 30 * 60 * 1000L
    }
}
