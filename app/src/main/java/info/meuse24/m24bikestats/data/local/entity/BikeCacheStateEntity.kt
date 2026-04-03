package info.meuse24.m24bikestats.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bike_cache_state")
data class BikeCacheStateEntity(
    @PrimaryKey val id: Int = CACHE_STATE_ID,
    val updatedAtEpochMillis: Long,
) {
    companion object {
        const val CACHE_STATE_ID = 0
    }
}
