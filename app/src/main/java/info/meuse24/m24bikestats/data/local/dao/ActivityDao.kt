package info.meuse24.m24bikestats.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import info.meuse24.m24bikestats.data.local.entity.ActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities ORDER BY startTime DESC")
    fun observeAll(): Flow<List<ActivityEntity>>

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
