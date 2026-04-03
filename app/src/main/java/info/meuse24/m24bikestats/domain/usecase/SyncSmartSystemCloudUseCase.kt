package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.SmartSystemCloudSyncSummary
import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository

class SyncSmartSystemCloudUseCase(
    private val repository: BoschSmartSystemRepository,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(
        onActivityProgress: (loadedCount: Int, totalCount: Int) -> Unit = { _, _ -> },
    ): Result<SmartSystemCloudSyncSummary> {
        val token = authRepository.getValidAccessToken()
            .getOrElse { return Result.failure(it) }

        val bikes = repository.getBikes(token)
            .getOrElse { return Result.failure(it) }

        var loadedActivityCount = 0
        var totalActivityCount = 0
        var offset = 0

        do {
            val page = repository.getActivities(
                accessToken = token,
                limit = SYNC_PAGE_SIZE,
                offset = offset,
            ).getOrElse { return Result.failure(it) }

            totalActivityCount = page.total.coerceAtLeast(loadedActivityCount)
            if (page.items.isEmpty()) {
                break
            }

            loadedActivityCount += page.items.size
            totalActivityCount = page.total.coerceAtLeast(loadedActivityCount)
            onActivityProgress(loadedActivityCount, totalActivityCount)
            offset = page.offset + page.items.size
        } while (loadedActivityCount < totalActivityCount)

        return Result.success(
            SmartSystemCloudSyncSummary(
                activityCount = loadedActivityCount,
                bikeCount = bikes.size,
            ),
        )
    }

    private companion object {
        private const val SYNC_PAGE_SIZE = 100
    }
}
