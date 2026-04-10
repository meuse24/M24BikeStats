package info.meuse24.m24bikestats.domain.repository

import info.meuse24.m24bikestats.domain.model.ActivityDetailCacheOverview
import info.meuse24.m24bikestats.domain.model.CloudSyncDetailMode
import kotlinx.coroutines.flow.Flow

interface BoschSmartSystemCacheStatusRepository {
    fun observeCachedActivityDetailCacheOverview(): Flow<ActivityDetailCacheOverview>
    fun observeActivityCacheUpdatedAtEpochMillis(): Flow<Long?>
    fun observeBikeCacheUpdatedAtEpochMillis(): Flow<Long?>
    fun observeActivityDetailCacheUpdatedAtEpochMillis(): Flow<Long?>
    suspend fun getCachedActivityTotalCount(): Int?
    suspend fun hasFreshActivities(maxAgeMillis: Long): Boolean
    suspend fun hasFreshActivityDetail(activityId: String, maxAgeMillis: Long): Boolean
    suspend fun hasFreshBikes(maxAgeMillis: Long): Boolean
    suspend fun hasFreshBikeDetail(bikeId: String, maxAgeMillis: Long): Boolean
    suspend fun getActivityIdsNeedingDetailSync(
        detailMode: CloudSyncDetailMode,
    ): List<String>
}
