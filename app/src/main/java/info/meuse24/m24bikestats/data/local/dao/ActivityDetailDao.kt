package info.meuse24.m24bikestats.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import info.meuse24.m24bikestats.data.local.entity.ActivityDetailEntity
import info.meuse24.m24bikestats.data.local.entity.ActivityDetailPointEntity
import info.meuse24.m24bikestats.data.local.model.CachedActivityDetail

@Dao
interface ActivityDetailDao {
    @Transaction
    @Query("SELECT * FROM activity_details WHERE activityId = :activityId LIMIT 1")
    suspend fun getByActivityId(activityId: String): CachedActivityDetail?

    @Upsert
    suspend fun upsertDetail(detail: ActivityDetailEntity)

    @Upsert
    suspend fun upsertPoints(points: List<ActivityDetailPointEntity>)

    @Query("DELETE FROM activity_detail_points WHERE activityId = :activityId")
    suspend fun deletePointsByActivityId(activityId: String)

    @Query("DELETE FROM activity_details WHERE activityId = :activityId")
    suspend fun deleteDetailByActivityId(activityId: String)

    @Transaction
    suspend fun replaceDetail(
        detail: ActivityDetailEntity,
        points: List<ActivityDetailPointEntity>,
    ) {
        deletePointsByActivityId(detail.activityId)
        deleteDetailByActivityId(detail.activityId)
        upsertDetail(detail)
        if (points.isNotEmpty()) {
            upsertPoints(points)
        }
    }
}
