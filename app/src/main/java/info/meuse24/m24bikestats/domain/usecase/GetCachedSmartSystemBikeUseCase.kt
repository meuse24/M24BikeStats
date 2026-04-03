package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.BoschBike
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository

class GetCachedSmartSystemBikeUseCase(
    private val repository: BoschSmartSystemRepository,
) {
    suspend operator fun invoke(bikeId: String): BoschBike? =
        repository.getCachedBike(bikeId)
}
