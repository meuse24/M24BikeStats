package info.meuse24.m24bikestats.data.local.mapper

import info.meuse24.m24bikestats.data.local.entity.BikeAssistModeEntity
import info.meuse24.m24bikestats.data.local.entity.BikeBatteryEntity
import info.meuse24.m24bikestats.data.local.entity.BikeEntity
import info.meuse24.m24bikestats.data.local.model.CachedBike
import info.meuse24.m24bikestats.domain.model.BoschAssistMode
import info.meuse24.m24bikestats.domain.model.BoschBattery
import info.meuse24.m24bikestats.domain.model.BoschBike
import info.meuse24.m24bikestats.domain.model.BoschComponent
import info.meuse24.m24bikestats.domain.model.BoschDriveUnit

fun CachedBike.toDomain(): BoschBike =
    BoschBike(
        id = bike.id,
        createdAt = bike.createdAt,
        language = bike.language,
        driveUnit = BoschDriveUnit(
            serialNumber = bike.driveUnitSerialNumber,
            partNumber = bike.driveUnitPartNumber,
            productName = bike.driveUnitProductName,
            odometerMeters = bike.driveUnitOdometerMeters,
            rearWheelCircumferenceMillimeters = bike.driveUnitRearWheelCircumferenceMillimeters,
            maximumAssistanceSpeedKmh = bike.driveUnitMaximumAssistanceSpeedKmh,
            walkAssistEnabled = bike.driveUnitWalkAssistEnabled,
            walkAssistMaximumSpeedKmh = bike.driveUnitWalkAssistMaximumSpeedKmh,
            activeAssistModes = assistModes
                .sortedBy { it.modeIndex }
                .map { BoschAssistMode(name = it.name, reachableRangeKm = it.reachableRangeKm) },
            totalPowerOnHours = bike.driveUnitTotalPowerOnHours,
            supportPowerOnHours = bike.driveUnitSupportPowerOnHours,
        ).takeIf {
            listOf(
                it.serialNumber,
                it.partNumber,
                it.productName,
                it.odometerMeters,
                it.rearWheelCircumferenceMillimeters,
                it.maximumAssistanceSpeedKmh,
                it.walkAssistEnabled,
                it.walkAssistMaximumSpeedKmh,
                it.totalPowerOnHours,
                it.supportPowerOnHours,
            ).any { value -> value != null } || it.activeAssistModes.isNotEmpty()
        },
        remoteControl = BoschComponent(
            serialNumber = bike.remoteControlSerialNumber,
            partNumber = bike.remoteControlPartNumber,
            productName = bike.remoteControlProductName,
        ).takeIf { it.serialNumber != null || it.partNumber != null || it.productName != null },
        headUnit = BoschComponent(
            serialNumber = bike.headUnitSerialNumber,
            partNumber = bike.headUnitPartNumber,
            productName = bike.headUnitProductName,
        ).takeIf { it.serialNumber != null || it.partNumber != null || it.productName != null },
        batteries = batteries
            .sortedBy { it.batteryIndex }
            .map {
                BoschBattery(
                    serialNumber = it.serialNumber,
                    partNumber = it.partNumber,
                    productName = it.productName,
                    deliveredWhOverLifetime = it.deliveredWhOverLifetime,
                    totalChargeCycles = it.totalChargeCycles,
                    onBikeChargeCycles = it.onBikeChargeCycles,
                    offBikeChargeCycles = it.offBikeChargeCycles,
                )
            },
    )

fun BoschBike.toEntity(): BikeEntity =
    BikeEntity(
        id = id,
        createdAt = createdAt,
        language = language,
        driveUnitSerialNumber = driveUnit?.serialNumber,
        driveUnitPartNumber = driveUnit?.partNumber,
        driveUnitProductName = driveUnit?.productName,
        driveUnitOdometerMeters = driveUnit?.odometerMeters,
        driveUnitRearWheelCircumferenceMillimeters = driveUnit?.rearWheelCircumferenceMillimeters,
        driveUnitMaximumAssistanceSpeedKmh = driveUnit?.maximumAssistanceSpeedKmh,
        driveUnitWalkAssistEnabled = driveUnit?.walkAssistEnabled,
        driveUnitWalkAssistMaximumSpeedKmh = driveUnit?.walkAssistMaximumSpeedKmh,
        driveUnitTotalPowerOnHours = driveUnit?.totalPowerOnHours,
        driveUnitSupportPowerOnHours = driveUnit?.supportPowerOnHours,
        remoteControlSerialNumber = remoteControl?.serialNumber,
        remoteControlPartNumber = remoteControl?.partNumber,
        remoteControlProductName = remoteControl?.productName,
        headUnitSerialNumber = headUnit?.serialNumber,
        headUnitPartNumber = headUnit?.partNumber,
        headUnitProductName = headUnit?.productName,
    )

fun BoschBike.toBatteryEntities(): List<BikeBatteryEntity> =
    batteries.mapIndexed { index, battery ->
        BikeBatteryEntity(
            bikeId = id,
            batteryIndex = index,
            serialNumber = battery.serialNumber,
            partNumber = battery.partNumber,
            productName = battery.productName,
            deliveredWhOverLifetime = battery.deliveredWhOverLifetime,
            totalChargeCycles = battery.totalChargeCycles,
            onBikeChargeCycles = battery.onBikeChargeCycles,
            offBikeChargeCycles = battery.offBikeChargeCycles,
        )
    }

fun BoschBike.toAssistModeEntities(): List<BikeAssistModeEntity> =
    driveUnit?.activeAssistModes.orEmpty().mapIndexed { index, mode ->
        BikeAssistModeEntity(
            bikeId = id,
            modeIndex = index,
            name = mode.name,
            reachableRangeKm = mode.reachableRangeKm,
        )
    }
