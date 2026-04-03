package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.BoschBike
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository
import kotlinx.coroutines.flow.Flow

class ObserveCachedSmartSystemBikesUseCase(
    private val repository: BoschSmartSystemRepository,
) {
    operator fun invoke(): Flow<List<BoschBike>> = repository.observeCachedBikes()
}
