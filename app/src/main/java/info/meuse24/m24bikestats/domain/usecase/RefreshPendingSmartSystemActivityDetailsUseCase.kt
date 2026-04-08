package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.CloudSyncDetailMode
import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemCacheStatusRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

class RefreshPendingSmartSystemActivityDetailsUseCase(
    private val repository: BoschSmartSystemRepository,
    private val cacheStatusRepository: BoschSmartSystemCacheStatusRepository,
    private val authRepository: AuthRepository,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val detailStaleThresholdMillis: Long = ACTIVITY_DETAIL_CACHE_TTL_MS,
) {
    suspend fun refreshMissing(
        onProgress: (processedCount: Int, totalCount: Int) -> Unit = { _, _ -> },
    ): Result<Int> = refresh(RefreshScope.MISSING_ONLY, onProgress)

    suspend fun refreshStale(
        onProgress: (processedCount: Int, totalCount: Int) -> Unit = { _, _ -> },
    ): Result<Int> = refresh(RefreshScope.STALE_ONLY, onProgress)

    private suspend fun refresh(
        scope: RefreshScope,
        onProgress: (processedCount: Int, totalCount: Int) -> Unit,
    ): Result<Int> = withValidAccessToken(authRepository) { token ->
        val staleThresholdEpochMillis = nowMillis() - detailStaleThresholdMillis
        val missingIds = cacheStatusRepository.getActivityIdsNeedingDetailSync(
            detailMode = CloudSyncDetailMode.MISSING_ONLY,
            staleThresholdEpochMillis = staleThresholdEpochMillis,
        )
        val candidateIds = when (scope) {
            RefreshScope.MISSING_ONLY -> missingIds
            RefreshScope.STALE_ONLY -> {
                val missingIdSet = missingIds.toSet()
                cacheStatusRepository.getActivityIdsNeedingDetailSync(
                    detailMode = CloudSyncDetailMode.MISSING_OR_STALE,
                    staleThresholdEpochMillis = staleThresholdEpochMillis,
                ).filterNot(missingIdSet::contains)
            }
        }

        if (candidateIds.isNotEmpty()) {
            onProgress(0, candidateIds.size)
        }

        candidateIds.forEachIndexed { index, activityId ->
            coroutineContext.ensureActive()
            repository.getActivityDetail(token, activityId)
                .getOrElse { return@withValidAccessToken Result.failure(it) }
            onProgress(index + 1, candidateIds.size)
        }

        Result.success(candidateIds.size)
    }

    private enum class RefreshScope {
        MISSING_ONLY,
        STALE_ONLY,
    }
}
