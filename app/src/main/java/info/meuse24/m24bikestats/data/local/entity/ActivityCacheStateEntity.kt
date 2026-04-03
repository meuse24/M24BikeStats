package info.meuse24.m24bikestats.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_cache_state")
data class ActivityCacheStateEntity(
    @PrimaryKey val id: Int = CACHE_STATE_ID,
    val totalCount: Int,
    val updatedAtEpochMillis: Long,
) {
    companion object {
        const val CACHE_STATE_ID = 0
    }
}
