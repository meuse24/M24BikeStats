package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.BoschBike
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository
import kotlinx.coroutines.flow.Flow

class ObserveCachedSmartSystemBikeDetailUseCase(
    private val repository: BoschSmartSystemRepository,
) {
    operator fun invoke(bikeId: String): Flow<BoschBike?> =
        repository.observeCachedBike(bikeId)
}
