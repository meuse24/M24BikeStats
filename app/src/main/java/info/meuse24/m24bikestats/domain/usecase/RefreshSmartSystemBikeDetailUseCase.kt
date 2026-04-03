package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.BoschBike
import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository

class RefreshSmartSystemBikeDetailUseCase(
    private val repository: BoschSmartSystemRepository,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(
        bikeId: String,
        force: Boolean = false,
    ): Result<BoschBike?> {
        if (!force && repository.isBikeDetailCacheFresh(bikeId, CACHE_TTL_MS)) {
            return Result.success(null)
        }

        val token = authRepository.getValidAccessToken()
            .getOrElse { return Result.failure(it) }
        return repository.getBikeDetail(token, bikeId).map { it }
    }

    private companion object {
        const val CACHE_TTL_MS = 30 * 60 * 1000L
    }
}
