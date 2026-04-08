package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.CloudSyncDetailMode
import info.meuse24.m24bikestats.domain.model.DataStatusOverview
import info.meuse24.m24bikestats.domain.model.DataStatusState
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemCacheStatusRepository
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveDataStatusOverviewUseCase(
    private val repository: BoschSmartSystemRepository,
    private val cacheStatusRepository: BoschSmartSystemCacheStatusRepository,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val zoneIdProvider: () -> ZoneId = ZoneId::systemDefault,
    private val detailStaleThresholdMillis: Long = ACTIVITY_DETAIL_CACHE_TTL_MS,
) {
    operator fun invoke(): Flow<DataStatusOverview> =
        combine(
            repository.observeCachedActivities(),
            cacheStatusRepository.observeCachedActivityDetailCacheOverview(),
            cacheStatusRepository.observeActivityCacheUpdatedAtEpochMillis(),
            cacheStatusRepository.observeBikeCacheUpdatedAtEpochMillis(),
            cacheStatusRepository.observeActivityDetailCacheUpdatedAtEpochMillis(),
        ) { activities, detailOverview, lastActivitySyncAt, lastBikeSyncAt, lastDetailSyncAt ->
            DataStatusInputs(
                activities = activities,
                detailOverview = detailOverview,
                lastActivitySyncAt = lastActivitySyncAt,
                lastBikeSyncAt = lastBikeSyncAt,
                lastDetailSyncAt = lastDetailSyncAt,
            )
        }.mapLatest { inputs ->
            val zoneId = zoneIdProvider()
            val staleThresholdEpochMillis = nowMillis() - detailStaleThresholdMillis
            val missingDetailIds = cacheStatusRepository.getActivityIdsNeedingDetailSync(
                detailMode = CloudSyncDetailMode.MISSING_ONLY,
                staleThresholdEpochMillis = staleThresholdEpochMillis,
            )
            val missingDetailCount = missingDetailIds.size
            val missingDetailIdSet = missingDetailIds.toSet()
            val staleOrMissingIds = cacheStatusRepository.getActivityIdsNeedingDetailSync(
                detailMode = CloudSyncDetailMode.MISSING_OR_STALE,
                staleThresholdEpochMillis = staleThresholdEpochMillis,
            )
            val staleDetailCount = staleOrMissingIds.count { it !in missingDetailIdSet }
            val coveredDates = inputs.activities.mapNotNull { activity ->
                runCatching {
                    Instant.parse(activity.startTime).atZone(zoneId).toLocalDate()
                }.getOrNull()
            }

            DataStatusOverview(
                cachedActivityCount = inputs.activities.size,
                coveredActivityStartEpochMillis = coveredDates.minOrNull()
                    ?.atStartOfDay(zoneId)
                    ?.toInstant()
                    ?.toEpochMilli(),
                coveredActivityEndEpochMillis = coveredDates.maxOrNull()
                    ?.atStartOfDay(zoneId)
                    ?.toInstant()
                    ?.toEpochMilli(),
                detailedActivityCount = inputs.detailOverview.detailedActivityCount,
                missingDetailCount = missingDetailCount,
                staleDetailCount = staleDetailCount,
                gpsPointCount = inputs.detailOverview.gpsPointCount,
                lastActivitySyncAtEpochMillis = inputs.lastActivitySyncAt,
                lastBikeSyncAtEpochMillis = inputs.lastBikeSyncAt,
                lastDetailSyncAtEpochMillis = inputs.lastDetailSyncAt,
                status = when {
                    inputs.activities.isEmpty() -> DataStatusState.EMPTY
                    missingDetailCount > 0 -> DataStatusState.PARTIAL
                    staleDetailCount > 0 -> DataStatusState.STALE
                    else -> DataStatusState.COMPLETE
                },
            )
        }

    private data class DataStatusInputs(
        val activities: List<info.meuse24.m24bikestats.domain.model.BoschActivity>,
        val detailOverview: info.meuse24.m24bikestats.domain.model.ActivityDetailCacheOverview,
        val lastActivitySyncAt: Long?,
        val lastBikeSyncAt: Long?,
        val lastDetailSyncAt: Long?,
    )
}
