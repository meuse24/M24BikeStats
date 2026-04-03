package info.meuse24.m24bikestats.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey val id: String,
    val title: String,
    val startTime: String,
    val startTimeEpoch: Long,
    val endTime: String?,
    val timeZone: String?,
    val durationWithoutStopsSeconds: Int,
    val bikeId: String?,
    val startOdometerMeters: Int?,
    val distanceMeters: Int,
    val averageSpeedKmh: Double?,
    val maxSpeedKmh: Double?,
    val averageCadenceRpm: Double?,
    val maxCadenceRpm: Double?,
    val averageRiderPowerWatts: Double?,
    val maxRiderPowerWatts: Double?,
    val elevationGainMeters: Int?,
    val elevationLossMeters: Int?,
    val caloriesBurned: Double?,
)
