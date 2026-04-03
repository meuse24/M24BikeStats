package info.meuse24.m24bikestats.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import info.meuse24.m24bikestats.data.local.entity.ActivityDetailEntity
import info.meuse24.m24bikestats.data.local.entity.ActivityDetailPointEntity
import info.meuse24.m24bikestats.data.local.model.ActivityDetailCacheOverviewProjection
import info.meuse24.m24bikestats.data.local.model.CachedActivityDetail
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDetailDao {
    @Query(
        """
        SELECT
            COUNT(*) AS detailedActivityCount,
            COALESCE(SUM(pointCount), 0) AS detailPointCount,
            COALESCE(SUM(gpsPointCount), 0) AS gpsPointCount
        FROM activity_details
        """
    )
    fun observeCacheOverview(): Flow<ActivityDetailCacheOverviewProjection>

    @Transaction
    @Query("SELECT * FROM activity_details WHERE activityId = :activityId LIMIT 1")
    fun observeByActivityId(activityId: String): Flow<CachedActivityDetail?>

    @Transaction
    @Query("SELECT * FROM activity_details WHERE activityId = :activityId LIMIT 1")
    suspend fun getByActivityId(activityId: String): CachedActivityDetail?

    @Query("SELECT updatedAtEpochMillis FROM activity_details WHERE activityId = :activityId LIMIT 1")
    suspend fun getUpdatedAtEpochMillis(activityId: String): Long?

    @Query("SELECT * FROM activity_details")
    suspend fun getAllMetadata(): List<ActivityDetailEntity>

    @Upsert
    suspend fun upsertDetail(detail: ActivityDetailEntity)

    @Upsert
    suspend fun upsertPoints(points: List<ActivityDetailPointEntity>)

    @Query("DELETE FROM activity_details WHERE activityId = :activityId")
    suspend fun deleteDetailByActivityId(activityId: String)

    @Transaction
    suspend fun replaceDetail(
        detail: ActivityDetailEntity,
        points: List<ActivityDetailPointEntity>,
    ) {
        deleteDetailByActivityId(detail.activityId)
        upsertDetail(detail)
        if (points.isNotEmpty()) {
            upsertPoints(points)
        }
    }
}
