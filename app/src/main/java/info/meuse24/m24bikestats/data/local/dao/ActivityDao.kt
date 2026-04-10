package info.meuse24.m24bikestats.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import info.meuse24.m24bikestats.data.local.entity.ActivityCacheStateEntity
import info.meuse24.m24bikestats.data.local.entity.ActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities ORDER BY startTimeEpoch DESC, startTime DESC")
    fun observeAll(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE id = :activityId LIMIT 1")
    suspend fun getById(activityId: String): ActivityEntity?

    @Query("SELECT * FROM activities ORDER BY startTimeEpoch DESC, startTime DESC")
    suspend fun getAll(): List<ActivityEntity>

    @Query("SELECT COUNT(*) FROM activities")
    suspend fun count(): Int

    @Query("SELECT totalCount FROM activity_cache_state WHERE id = 0 LIMIT 1")
    suspend fun getCachedTotalCount(): Int?

    @Query("SELECT updatedAtEpochMillis FROM activity_cache_state WHERE id = 0 LIMIT 1")
    suspend fun getCacheUpdatedAtEpochMillis(): Long?

    @Query("SELECT updatedAtEpochMillis FROM activity_cache_state WHERE id = 0 LIMIT 1")
    fun observeCacheUpdatedAtEpochMillis(): Flow<Long?>

    @Upsert
    suspend fun upsertCacheState(cacheState: ActivityCacheStateEntity)

    @Upsert
    suspend fun upsertAll(activities: List<ActivityEntity>)

    @Query("SELECT * FROM activities WHERE centerLatitude IS NOT NULL")
    suspend fun getAllWithCenter(): List<ActivityEntity>

    @Transaction
    suspend fun upsertAllPreservingCenter(activities: List<ActivityEntity>) {
        val existingCenters = getAllWithCenter()
            .associate { it.id to (it.centerLatitude!! to it.centerLongitude!!) }
        upsertAll(activities)
        for ((id, center) in existingCenters) {
            updateCenter(id, center.first, center.second)
        }
    }

    @Query("DELETE FROM activities")
    suspend fun clearAll()

    @Query("DELETE FROM activity_cache_state")
    suspend fun clearCacheState()

    @Transaction
    suspend fun replaceAll(activities: List<ActivityEntity>) {
        clearAll()
        upsertAll(activities)
    }

    @Query("""
        UPDATE activities
        SET centerLatitude = :lat, centerLongitude = :lng
        WHERE id = :activityId
    """)
    suspend fun updateCenter(activityId: String, lat: Double, lng: Double)

    @Query("""
        UPDATE activities
        SET centerLatitude = NULL, centerLongitude = NULL
        WHERE id = :activityId
    """)
    suspend fun clearCenter(activityId: String)

    @Query("""
        SELECT id
        FROM activities
        WHERE centerLatitude IS NULL
    """)
    suspend fun getIdsWithoutCenter(): List<String>

    @Query(
        """
        SELECT a.id
        FROM activities a
        INNER JOIN activity_details d ON d.activityId = a.id
        WHERE a.centerLatitude IS NOT NULL
          AND NOT EXISTS (
              SELECT 1
              FROM activity_detail_points p
              WHERE p.activityId = a.id
                AND p.latitude IS NOT NULL
                AND p.longitude IS NOT NULL
          )
        """
    )
    suspend fun getIdsWithCenterButWithoutGpsPointsInDetails(): List<String>
}
