package info.meuse24.m24bikestats.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bikes")
data class BikeEntity(
    @PrimaryKey val id: String,
    val createdAt: String?,
    val language: String?,
    val driveUnitSerialNumber: String?,
    val driveUnitPartNumber: String?,
    val driveUnitProductName: String?,
    val driveUnitOdometerMeters: Double?,
    val driveUnitRearWheelCircumferenceMillimeters: Double?,
    val driveUnitMaximumAssistanceSpeedKmh: Double?,
    val driveUnitWalkAssistEnabled: Boolean?,
    val driveUnitWalkAssistMaximumSpeedKmh: Double?,
    val driveUnitTotalPowerOnHours: Int?,
    val driveUnitSupportPowerOnHours: Int?,
    val remoteControlSerialNumber: String?,
    val remoteControlPartNumber: String?,
    val remoteControlProductName: String?,
    val headUnitSerialNumber: String?,
    val headUnitPartNumber: String?,
    val headUnitProductName: String?,
)
