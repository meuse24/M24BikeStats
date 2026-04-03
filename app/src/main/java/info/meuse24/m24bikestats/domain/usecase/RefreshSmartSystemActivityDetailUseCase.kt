package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.BoschActivityDetail
import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository

class RefreshSmartSystemActivityDetailUseCase(
    private val repository: BoschSmartSystemRepository,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(
        activityId: String,
        force: Boolean = false,
    ): Result<BoschActivityDetail?> {
        if (!force && repository.isActivityDetailCacheFresh(activityId, CACHE_TTL_MS)) {
            return Result.success(null)
        }

        val token = authRepository.getValidAccessToken()
            .getOrElse { return Result.failure(it) }
        return repository.getActivityDetail(token, activityId).map { it }
    }

    private companion object {
        const val CACHE_TTL_MS = 30 * 60 * 1000L
    }
}
