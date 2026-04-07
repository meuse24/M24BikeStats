package info.meuse24.m24bikestats.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "bike_passes",
    primaryKeys = ["bikeId"],
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
data class BikePassEntity(
    val bikeId: String,
    val frameNumber: String?,
    val frameNumberPosition: String?,
    val description: String?,
    val createdAt: String?,
    val updatedAt: String?,
)
