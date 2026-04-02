package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.BoschActivityPage
import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository

class GetSmartSystemActivitiesUseCase(
    private val repository: BoschSmartSystemRepository,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(limit: Int, offset: Int): Result<BoschActivityPage> {
        val token = authRepository.getValidAccessToken()
            .getOrElse { return Result.failure(it) }
        return repository.getActivities(token, limit = limit, offset = offset)
    }
}
