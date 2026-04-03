package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.BoschBike
import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository

class RefreshSmartSystemBikesUseCase(
    private val repository: BoschSmartSystemRepository,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(force: Boolean = false): Result<List<BoschBike>?> {
        if (!force && repository.isBikesCacheFresh(CACHE_TTL_MS)) {
            return Result.success(null)
        }

        val token = authRepository.getValidAccessToken()
            .getOrElse { return Result.failure(it) }
        return repository.getBikes(token).map { it }
    }

    private companion object {
        const val CACHE_TTL_MS = 5 * 60 * 1000L
    }
}
