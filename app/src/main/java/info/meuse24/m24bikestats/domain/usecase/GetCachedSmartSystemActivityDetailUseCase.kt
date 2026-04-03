package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.BoschActivityDetail
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository

class GetCachedSmartSystemActivityDetailUseCase(
    private val repository: BoschSmartSystemRepository,
) {
    suspend operator fun invoke(activityId: String): BoschActivityDetail? =
        repository.getCachedActivityDetail(activityId)
}
