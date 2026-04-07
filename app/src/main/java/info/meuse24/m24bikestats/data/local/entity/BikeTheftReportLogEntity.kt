package info.meuse24.m24bikestats.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "bike_theft_report_logs",
    primaryKeys = ["bikeId", "theftReportLogId"],
    foreignKeys = [
        ForeignKey(
            entity = BikeEntity::class,
            parentColumns = ["id"],
            childColumns = ["bikeId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("bikeId")],
)
data class BikeTheftReportLogEntity(
    val bikeId: String,
    val theftReportLogId: String,
    val createdAt: String?,
    val expiresAtEpochMillis: Long?,
    val timeZone: String?,
    val theftCaseEnteredAt: String?,
    val riderPortalLink: String?,
    val description: String?,
    val locationDetectedAt: String?,
    val locationLatitude: Double?,
    val locationLongitude: Double?,
    val locationHorizontalAccuracyMeters: Double?,
    val locationAddress: String?,
    val locationDescription: String?,
)
