package info.meuse24.m24bikestats.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import info.meuse24.m24bikestats.data.local.entity.BikeAssistModeEntity
import info.meuse24.m24bikestats.data.local.entity.BikeAbsEntity
import info.meuse24.m24bikestats.data.local.entity.BikeBatteryEntity
import info.meuse24.m24bikestats.data.local.entity.BikeEntity
import info.meuse24.m24bikestats.data.local.entity.BikePassEntity
import info.meuse24.m24bikestats.data.local.entity.BikeRegistrationEntity
import info.meuse24.m24bikestats.data.local.entity.BikeServiceRecordEntity
import info.meuse24.m24bikestats.data.local.entity.BikeTheftReportLogEntity

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
    @Relation(
        parentColumn = "id",
        entityColumn = "bikeId",
    )
    val antiLockBrakeSystems: List<BikeAbsEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "bikeId",
    )
    val bikePass: BikePassEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "bikeId",
    )
    val theftReportLogs: List<BikeTheftReportLogEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "bikeId",
    )
    val serviceRecords: List<BikeServiceRecordEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "bikeId",
    )
    val registrations: List<BikeRegistrationEntity>,
)
