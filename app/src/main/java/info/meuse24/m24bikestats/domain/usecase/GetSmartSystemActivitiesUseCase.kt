package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.BoschActivityPage
import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository

class GetSmartSystemActivitiesUseCase(
    private val repository: BoschSmartSystemRepository,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(limit: Int, offset: Int): Result<BoschActivityPage> =
        withValidAccessToken(authRepository) { token ->
            repository.getActivities(token, limit = limit, offset = offset)
        }
}
