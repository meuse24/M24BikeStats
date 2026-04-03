package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.ActivityDetailCacheOverview
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository
import kotlinx.coroutines.flow.Flow

class ObserveCachedSmartSystemActivityDetailCacheOverviewUseCase(
    private val repository: BoschSmartSystemRepository,
) {
    operator fun invoke(): Flow<ActivityDetailCacheOverview> =
        repository.observeCachedActivityDetailCacheOverview()
}
