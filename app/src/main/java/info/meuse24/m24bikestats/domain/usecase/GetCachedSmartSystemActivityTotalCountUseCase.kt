package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository

class GetCachedSmartSystemActivityTotalCountUseCase(
    private val repository: BoschSmartSystemRepository,
) {
    suspend operator fun invoke(): Int? =
        repository.getCachedActivityTotalCount()
}
