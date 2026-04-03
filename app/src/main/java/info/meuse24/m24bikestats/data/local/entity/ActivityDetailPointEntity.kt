package info.meuse24.m24bikestats.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "activity_detail_points",
    primaryKeys = ["activityId", "pointIndex"],
    foreignKeys = [
        ForeignKey(
            entity = ActivityDetailEntity::class,
            parentColumns = ["activityId"],
            childColumns = ["activityId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("activityId")],
)
data class ActivityDetailPointEntity(
    val activityId: String,
    val pointIndex: Int,
    val distanceMeters: Double?,
    val altitudeMeters: Double?,
    val speedKmh: Double?,
    val cadenceRpm: Double?,
    val latitude: Double?,
    val longitude: Double?,
    val riderPowerWatts: Double?,
)
