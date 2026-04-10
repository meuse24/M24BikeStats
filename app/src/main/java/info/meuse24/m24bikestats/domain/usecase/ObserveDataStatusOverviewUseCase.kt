package info.meuse24.m24bikestats.domain.usecase

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
    private val zoneIdProvider: () -> ZoneId = ZoneId::systemDefault,
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
            val missingDetailIds = cacheStatusRepository.getActivityIdsNeedingDetailSync(
                detailMode = info.meuse24.m24bikestats.domain.model.CloudSyncDetailMode.MISSING_ONLY,
                staleThresholdEpochMillis = 0L,
            )
            val missingDetailCount = missingDetailIds.size
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
                gpsPointCount = inputs.detailOverview.gpsPointCount,
                lastActivitySyncAtEpochMillis = inputs.lastActivitySyncAt,
                lastBikeSyncAtEpochMillis = inputs.lastBikeSyncAt,
                lastDetailSyncAtEpochMillis = inputs.lastDetailSyncAt,
                status = when {
                    inputs.activities.isEmpty() -> DataStatusState.EMPTY
                    missingDetailCount > 0 -> DataStatusState.PARTIAL
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
