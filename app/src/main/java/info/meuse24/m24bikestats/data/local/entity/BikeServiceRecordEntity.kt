package info.meuse24.m24bikestats.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "bike_service_records",
    primaryKeys = ["bikeId", "serviceRecordId"],
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
data class BikeServiceRecordEntity(
    val bikeId: String,
    val serviceRecordId: String,
    val type: String,
    val createdAt: String,
    val odometerValueMeters: Long?,
    val bikeDealerName: String?,
    val bikeDealerCity: String?,
    val toolVersion: String?,
    val batteryFullChargeCycles: Int?,
    val batteryMeasuredEnergyCapacityWh: Int?,
    val batteryNominalEnergyCapacityWh: Int?,
    val batteryMeasuredCapacityPercentage: Int?,
    val batteryOnBikeMeasurement: Boolean?,
    val softwareUpdateClientType: String?,
    val softwareUpdateClientVersion: String?,
    val softwareUpdateForced: Boolean?,
    val softwareUpdateUpdatedComponentsCount: Int?,
    val softwareUpdateUpdatedComponentNames: String?,
)
