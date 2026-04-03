package info.meuse24.m24bikestats.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "activity_details",
    foreignKeys = [
        ForeignKey(
            entity = ActivityEntity::class,
            parentColumns = ["id"],
            childColumns = ["activityId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("activityId")],
)
data class ActivityDetailEntity(
    @PrimaryKey val activityId: String,
    val pointCount: Int,
    val gpsPointCount: Int,
    val updatedAtEpochMillis: Long,
)
