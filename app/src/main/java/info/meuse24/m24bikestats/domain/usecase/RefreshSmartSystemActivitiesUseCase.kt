package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.BoschActivityPage
import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository

class RefreshSmartSystemActivitiesUseCase(
    private val repository: BoschSmartSystemRepository,
    private val authRepository: AuthRepository,
    private val cacheTtlMillis: Long = DEFAULT_CACHE_TTL_MS,
) {
    suspend operator fun invoke(
        limit: Int,
        offset: Int,
        force: Boolean = false,
    ): Result<BoschActivityPage?> {
        if (offset == 0 && !force && repository.isActivitiesCacheFresh(cacheTtlMillis)) {
            return Result.success(null)
        }

        val token = authRepository.getValidAccessToken()
            .getOrElse { return Result.failure(it) }
        return repository.getActivities(token, limit = limit, offset = offset).map { it }
    }

    private companion object {
        const val DEFAULT_CACHE_TTL_MS = 5 * 60 * 1000L
    }
}
