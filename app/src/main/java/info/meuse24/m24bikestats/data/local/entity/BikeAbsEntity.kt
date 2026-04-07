package info.meuse24.m24bikestats.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "bike_abs_components",
    primaryKeys = ["bikeId", "absIndex"],
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
data class BikeAbsEntity(
    val bikeId: String,
    val absIndex: Int,
    val serialNumber: String?,
    val partNumber: String?,
    val productName: String?,
)
