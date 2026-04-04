package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.CloudSyncDetailMode
import info.meuse24.m24bikestats.domain.model.SmartSystemCloudSyncPhase
import info.meuse24.m24bikestats.domain.model.SmartSystemCloudSyncProgress
import info.meuse24.m24bikestats.domain.model.SmartSystemCloudSyncSummary
import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemCacheStatusRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

class SyncSmartSystemCloudUseCase(
    private val repository: BoschSmartSystemRepository,
    private val cacheStatusRepository: BoschSmartSystemCacheStatusRepository,
    private val authRepository: AuthRepository,
    private val activityDetailCacheTtlMillis: Long = DEFAULT_ACTIVITY_DETAIL_CACHE_TTL_MS,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    suspend operator fun invoke(
        detailMode: CloudSyncDetailMode,
        onProgress: (SmartSystemCloudSyncProgress) -> Unit = {},
    ): Result<SmartSystemCloudSyncSummary> = withValidAccessToken(authRepository) { token ->
        onProgress(
            SmartSystemCloudSyncProgress(
                phase = SmartSystemCloudSyncPhase.BIKES,
                processedCount = 0,
                totalCount = 1,
            )
        )
        val bikes = repository.getBikes(token)
            .getOrElse { return@withValidAccessToken Result.failure(it) }
        onProgress(
            SmartSystemCloudSyncProgress(
                phase = SmartSystemCloudSyncPhase.BIKES,
                processedCount = 1,
                totalCount = 1,
            )
        )

        val knownActivityIds = repository.getCachedActivities()
            .asSequence()
            .map { it.id }
            .toMutableSet()
        var offset = 0
        var totalActivityCount = cacheStatusRepository.getCachedActivityTotalCount() ?: knownActivityIds.size

        do {
            coroutineContext.ensureActive()
            val page = repository.getActivities(
                accessToken = token,
                limit = SYNC_PAGE_SIZE,
                offset = offset,
            ).getOrElse { return@withValidAccessToken Result.failure(it) }

            totalActivityCount = page.total
            if (page.items.isEmpty()) {
                break
            }

            knownActivityIds += page.items.map { it.id }
            onProgress(
                SmartSystemCloudSyncProgress(
                    phase = SmartSystemCloudSyncPhase.ACTIVITIES,
                    processedCount = knownActivityIds.size.coerceAtMost(totalActivityCount),
                    totalCount = totalActivityCount,
                )
            )
            offset = page.offset + page.items.size
        } while (offset < totalActivityCount && knownActivityIds.size < totalActivityCount)

        val detailCandidates = cacheStatusRepository.getActivityIdsNeedingDetailSync(
            detailMode = detailMode,
            staleThresholdEpochMillis = nowMillis() - activityDetailCacheTtlMillis,
        )

        if (detailCandidates.isNotEmpty()) {
            onProgress(
                SmartSystemCloudSyncProgress(
                    phase = SmartSystemCloudSyncPhase.ACTIVITY_DETAILS,
                    processedCount = 0,
                    totalCount = detailCandidates.size,
                )
            )
        }

        detailCandidates.forEachIndexed { index, activityId ->
            coroutineContext.ensureActive()
            repository.getActivityDetail(token, activityId)
                .getOrElse { return@withValidAccessToken Result.failure(it) }
            onProgress(
                SmartSystemCloudSyncProgress(
                    phase = SmartSystemCloudSyncPhase.ACTIVITY_DETAILS,
                    processedCount = index + 1,
                    totalCount = detailCandidates.size,
                )
            )
        }

        Result.success(
            SmartSystemCloudSyncSummary(
                activityCount = repository.getCachedActivities().size,
                bikeCount = bikes.size,
            ),
        )
    }

    private companion object {
        private const val SYNC_PAGE_SIZE = 100
        private const val DEFAULT_ACTIVITY_DETAIL_CACHE_TTL_MS = 30 * 60 * 1000L
    }
}
