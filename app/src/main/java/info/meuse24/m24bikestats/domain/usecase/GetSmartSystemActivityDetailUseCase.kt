package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.BoschActivityDetail
import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository

class GetSmartSystemActivityDetailUseCase(
    private val repository: BoschSmartSystemRepository,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(activityId: String): Result<BoschActivityDetail> =
        withValidAccessToken(authRepository) { token ->
            repository.getActivityDetail(token, activityId)
        }
}
