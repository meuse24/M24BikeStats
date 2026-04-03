package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository
import kotlinx.coroutines.flow.Flow

class ObserveCachedSmartSystemActivitiesUseCase(
    private val repository: BoschSmartSystemRepository,
) {
    operator fun invoke(): Flow<List<BoschActivity>> = repository.observeCachedActivities()
}
