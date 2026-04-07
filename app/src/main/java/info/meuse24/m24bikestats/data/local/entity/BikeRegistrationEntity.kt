package info.meuse24.m24bikestats.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "bike_registrations",
    primaryKeys = ["bikeId", "registrationKey"],
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
data class BikeRegistrationEntity(
    val bikeId: String,
    val registrationKey: String,
    val registrationType: String,
    val createdAt: String,
    val componentType: String?,
    val partNumber: String?,
    val serialNumber: String?,
)
