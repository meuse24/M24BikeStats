package info.meuse24.m24bikestats.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import info.meuse24.m24bikestats.data.local.entity.BikeAssistModeEntity
import info.meuse24.m24bikestats.data.local.entity.BikeBatteryEntity
import info.meuse24.m24bikestats.data.local.entity.BikeEntity

data class CachedBike(
    @Embedded val bike: BikeEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "bikeId",
    )
    val batteries: List<BikeBatteryEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "bikeId",
    )
    val assistModes: List<BikeAssistModeEntity>,
)
