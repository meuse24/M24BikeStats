package info.meuse24.m24bikestats.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "bike_batteries",
    primaryKeys = ["bikeId", "batteryIndex"],
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
data class BikeBatteryEntity(
    val bikeId: String,
    val batteryIndex: Int,
    val serialNumber: String?,
    val partNumber: String?,
    val productName: String?,
    val deliveredWhOverLifetime: Int?,
    val totalChargeCycles: Double?,
    val onBikeChargeCycles: Double?,
    val offBikeChargeCycles: Double?,
)
