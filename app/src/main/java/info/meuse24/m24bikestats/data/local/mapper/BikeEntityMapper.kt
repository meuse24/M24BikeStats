package info.meuse24.m24bikestats.data.local.mapper

import info.meuse24.m24bikestats.data.local.entity.BikeAssistModeEntity
import info.meuse24.m24bikestats.data.local.entity.BikeAbsEntity
import info.meuse24.m24bikestats.data.local.entity.BikeBatteryEntity
import info.meuse24.m24bikestats.data.local.entity.BikeEntity
import info.meuse24.m24bikestats.data.local.entity.BikePassEntity
import info.meuse24.m24bikestats.data.local.entity.BikeRegistrationEntity
import info.meuse24.m24bikestats.data.local.entity.BikeServiceRecordEntity
import info.meuse24.m24bikestats.data.local.entity.BikeTheftReportLogEntity
import info.meuse24.m24bikestats.data.local.model.CachedBike
import info.meuse24.m24bikestats.domain.model.BoschAssistMode
import info.meuse24.m24bikestats.domain.model.BoschBattery
import info.meuse24.m24bikestats.domain.model.BoschBike
import info.meuse24.m24bikestats.domain.model.BoschBikePass
import info.meuse24.m24bikestats.domain.model.BoschComponent
import info.meuse24.m24bikestats.domain.model.BoschDriveUnit
import info.meuse24.m24bikestats.domain.model.BoschRegistration
import info.meuse24.m24bikestats.domain.model.BoschServiceRecord
import info.meuse24.m24bikestats.domain.model.BoschSoftwareUpdateSummary
import info.meuse24.m24bikestats.domain.model.BoschBatteryMeasurement
import info.meuse24.m24bikestats.domain.model.BoschTheftLocation
import info.meuse24.m24bikestats.domain.model.BoschTheftReportLog

fun CachedBike.toDomain(): BoschBike =
    BoschBike(
        id = bike.id,
        createdAt = bike.createdAt,
        language = bike.language,
        oemId = bike.oemId,
        serviceDueDate = bike.serviceDueDate,
        serviceDueOdometerMeters = bike.serviceDueOdometerMeters,
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
        connectModule = BoschComponent(
            serialNumber = bike.connectModuleSerialNumber,
            partNumber = bike.connectModulePartNumber,
            productName = bike.connectModuleProductName,
        ).takeIf { it.serialNumber != null || it.partNumber != null || it.productName != null },
        antiLockBrakeSystems = antiLockBrakeSystems
            .sortedBy { it.absIndex }
            .map { BoschComponent(serialNumber = it.serialNumber, partNumber = it.partNumber, productName = it.productName) },
        bikePass = bikePass?.let {
            BoschBikePass(
                bikeId = it.bikeId,
                frameNumber = it.frameNumber,
                frameNumberPosition = it.frameNumberPosition,
                description = it.description,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt,
            )
        },
        theftReportLogs = theftReportLogs
            .sortedByDescending { it.createdAt.orEmpty() }
            .map {
                BoschTheftReportLog(
                    theftReportLogId = it.theftReportLogId,
                    bikeId = it.bikeId,
                    createdAt = it.createdAt,
                    expiresAtEpochMillis = it.expiresAtEpochMillis,
                    timeZone = it.timeZone,
                    theftCaseEnteredAt = it.theftCaseEnteredAt,
                    riderPortalLink = it.riderPortalLink,
                    description = it.description,
                    location = BoschTheftLocation(
                        detectedAt = it.locationDetectedAt,
                        latitude = it.locationLatitude,
                        longitude = it.locationLongitude,
                        horizontalAccuracyMeters = it.locationHorizontalAccuracyMeters,
                        address = it.locationAddress,
                        description = it.locationDescription,
                    ).takeIf { location ->
                        listOf(
                            location.detectedAt,
                            location.latitude,
                            location.longitude,
                            location.horizontalAccuracyMeters,
                            location.address,
                            location.description,
                        ).any { value -> value != null }
                    },
                )
            },
        serviceRecords = serviceRecords
            .sortedByDescending { it.createdAt }
            .map {
                BoschServiceRecord(
                    id = it.serviceRecordId,
                    type = it.type,
                    bikeId = it.bikeId,
                    createdAt = it.createdAt,
                    odometerValueMeters = it.odometerValueMeters,
                    bikeDealerName = it.bikeDealerName,
                    bikeDealerCity = it.bikeDealerCity,
                    toolVersion = it.toolVersion,
                    batteryMeasurement = if (
                        it.batteryFullChargeCycles != null ||
                            it.batteryMeasuredEnergyCapacityWh != null ||
                            it.batteryNominalEnergyCapacityWh != null ||
                            it.batteryMeasuredCapacityPercentage != null ||
                            it.batteryOnBikeMeasurement != null
                    ) {
                        BoschBatteryMeasurement(
                            fullChargeCycles = it.batteryFullChargeCycles,
                            measuredEnergyCapacityWh = it.batteryMeasuredEnergyCapacityWh,
                            nominalEnergyCapacityWh = it.batteryNominalEnergyCapacityWh,
                            measuredCapacityPercentage = it.batteryMeasuredCapacityPercentage,
                            onBikeMeasurement = it.batteryOnBikeMeasurement,
                        )
                    } else {
                        null
                    },
                    softwareUpdate = if (
                        it.softwareUpdateClientType != null ||
                            it.softwareUpdateClientVersion != null ||
                            it.softwareUpdateForced != null ||
                            it.softwareUpdateUpdatedComponentsCount != null ||
                            !it.softwareUpdateUpdatedComponentNames.isNullOrBlank()
                    ) {
                        BoschSoftwareUpdateSummary(
                            clientType = it.softwareUpdateClientType,
                            clientVersion = it.softwareUpdateClientVersion,
                            isForcedUpdate = it.softwareUpdateForced,
                            updatedComponentsCount = it.softwareUpdateUpdatedComponentsCount ?: 0,
                            updatedComponentNames = it.softwareUpdateUpdatedComponentNames
                                ?.split('|')
                                ?.map { name -> name.trim() }
                                ?.filter { name -> name.isNotBlank() }
                                .orEmpty(),
                        )
                    } else {
                        null
                    },
                )
            },
        registrations = registrations
            .sortedByDescending { it.createdAt }
            .map {
                BoschRegistration(
                    registrationType = it.registrationType,
                    createdAt = it.createdAt,
                    bikeId = it.bikeId,
                    componentType = it.componentType,
                    partNumber = it.partNumber,
                    serialNumber = it.serialNumber,
                )
            },
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
    toEntity(updatedAtEpochMillis = System.currentTimeMillis())

fun BoschBike.toEntity(updatedAtEpochMillis: Long): BikeEntity =
    BikeEntity(
        id = id,
        createdAt = createdAt,
        updatedAtEpochMillis = updatedAtEpochMillis,
        language = language,
        oemId = oemId,
        serviceDueDate = serviceDueDate,
        serviceDueOdometerMeters = serviceDueOdometerMeters,
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
        connectModuleSerialNumber = connectModule?.serialNumber,
        connectModulePartNumber = connectModule?.partNumber,
        connectModuleProductName = connectModule?.productName,
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

fun BoschBike.toAbsEntities(): List<BikeAbsEntity> =
    antiLockBrakeSystems.mapIndexed { index, component ->
        BikeAbsEntity(
            bikeId = id,
            absIndex = index,
            serialNumber = component.serialNumber,
            partNumber = component.partNumber,
            productName = component.productName,
        )
    }

fun BoschBike.toBikePassEntity(): BikePassEntity? =
    bikePass?.let {
        BikePassEntity(
            bikeId = it.bikeId,
            frameNumber = it.frameNumber,
            frameNumberPosition = it.frameNumberPosition,
            description = it.description,
            createdAt = it.createdAt,
            updatedAt = it.updatedAt,
        )
    }

fun BoschBike.toTheftReportLogEntities(): List<BikeTheftReportLogEntity> =
    theftReportLogs.map { log ->
        BikeTheftReportLogEntity(
            bikeId = id,
            theftReportLogId = log.theftReportLogId,
            createdAt = log.createdAt,
            expiresAtEpochMillis = log.expiresAtEpochMillis,
            timeZone = log.timeZone,
            theftCaseEnteredAt = log.theftCaseEnteredAt,
            riderPortalLink = log.riderPortalLink,
            description = log.description,
            locationDetectedAt = log.location?.detectedAt,
            locationLatitude = log.location?.latitude,
            locationLongitude = log.location?.longitude,
            locationHorizontalAccuracyMeters = log.location?.horizontalAccuracyMeters,
            locationAddress = log.location?.address,
            locationDescription = log.location?.description,
        )
    }

fun BoschBike.toServiceRecordEntities(): List<BikeServiceRecordEntity> =
    serviceRecords.map { record ->
        BikeServiceRecordEntity(
            bikeId = id,
            serviceRecordId = record.id,
            type = record.type,
            createdAt = record.createdAt,
            odometerValueMeters = record.odometerValueMeters,
            bikeDealerName = record.bikeDealerName,
            bikeDealerCity = record.bikeDealerCity,
            toolVersion = record.toolVersion,
            batteryFullChargeCycles = record.batteryMeasurement?.fullChargeCycles,
            batteryMeasuredEnergyCapacityWh = record.batteryMeasurement?.measuredEnergyCapacityWh,
            batteryNominalEnergyCapacityWh = record.batteryMeasurement?.nominalEnergyCapacityWh,
            batteryMeasuredCapacityPercentage = record.batteryMeasurement?.measuredCapacityPercentage,
            batteryOnBikeMeasurement = record.batteryMeasurement?.onBikeMeasurement,
            softwareUpdateClientType = record.softwareUpdate?.clientType,
            softwareUpdateClientVersion = record.softwareUpdate?.clientVersion,
            softwareUpdateForced = record.softwareUpdate?.isForcedUpdate,
            softwareUpdateUpdatedComponentsCount = record.softwareUpdate?.updatedComponentsCount,
            softwareUpdateUpdatedComponentNames = record.softwareUpdate?.updatedComponentNames?.joinToString("|"),
        )
    }

fun BoschBike.toRegistrationEntities(): List<BikeRegistrationEntity> =
    registrations.map { registration ->
        BikeRegistrationEntity(
            bikeId = id,
            registrationKey = listOfNotNull(
                registration.registrationType,
                registration.createdAt,
                registration.componentType,
                registration.partNumber,
                registration.serialNumber,
            ).joinToString("|"),
            registrationType = registration.registrationType,
            createdAt = registration.createdAt,
            componentType = registration.componentType,
            partNumber = registration.partNumber,
            serialNumber = registration.serialNumber,
        )
    }
