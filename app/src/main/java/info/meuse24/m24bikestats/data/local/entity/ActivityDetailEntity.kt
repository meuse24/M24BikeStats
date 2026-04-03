package info.meuse24.m24bikestats.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_details")
data class ActivityDetailEntity(
    @PrimaryKey val activityId: String,
    val pointCount: Int,
    val gpsPointCount: Int,
    val updatedAtEpochMillis: Long,
)
