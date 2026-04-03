package info.meuse24.m24bikestats.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import info.meuse24.m24bikestats.data.local.entity.ActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities ORDER BY startTimeEpoch DESC, startTime DESC")
    fun observeAll(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE id = :activityId LIMIT 1")
    suspend fun getById(activityId: String): ActivityEntity?

    @Upsert
    suspend fun upsertAll(activities: List<ActivityEntity>)

    @Query("DELETE FROM activities")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(activities: List<ActivityEntity>) {
        clearAll()
        upsertAll(activities)
    }
}
