package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository

class GetCachedSmartSystemActivityUseCase(
    private val repository: BoschSmartSystemRepository,
) {
    suspend operator fun invoke(activityId: String): BoschActivity? =
        repository.getCachedActivity(activityId)
}
