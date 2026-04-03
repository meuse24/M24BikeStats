package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.BoschActivityDetail
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository
import kotlinx.coroutines.flow.Flow

class ObserveCachedSmartSystemActivityDetailUseCase(
    private val repository: BoschSmartSystemRepository,
) {
    operator fun invoke(activityId: String): Flow<BoschActivityDetail?> =
        repository.observeCachedActivityDetail(activityId)
}
