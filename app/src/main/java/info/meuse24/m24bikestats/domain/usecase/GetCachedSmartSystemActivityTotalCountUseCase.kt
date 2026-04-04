package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemCacheStatusRepository

class GetCachedSmartSystemActivityTotalCountUseCase(
    private val repository: BoschSmartSystemCacheStatusRepository,
) {
    suspend operator fun invoke(): Int? =
        repository.getCachedActivityTotalCount()
}
