package info.meuse24.m24bikestats.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "bike_assist_modes",
    primaryKeys = ["bikeId", "modeIndex"],
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
data class BikeAssistModeEntity(
    val bikeId: String,
    val modeIndex: Int,
    val name: String,
    val reachableRangeKm: Double?,
)
