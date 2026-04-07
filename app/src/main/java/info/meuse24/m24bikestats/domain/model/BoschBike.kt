package info.meuse24.m24bikestats.domain.model

data class BoschBike(
    val id: String,
    val createdAt: String?,
    val language: String?,
    val oemId: String? = null,
    val serviceDueDate: String? = null,
    val serviceDueOdometerMeters: Double? = null,
    val driveUnit: BoschDriveUnit?,
    val remoteControl: BoschComponent?,
    val headUnit: BoschComponent?,
    val connectModule: BoschComponent? = null,
    val antiLockBrakeSystems: List<BoschComponent> = emptyList(),
    val bikePass: BoschBikePass? = null,
    val theftReportLogs: List<BoschTheftReportLog> = emptyList(),
    val serviceRecords: List<BoschServiceRecord> = emptyList(),
    val registrations: List<BoschRegistration> = emptyList(),
    val batteries: List<BoschBattery>,
)

data class BoschDriveUnit(
    val serialNumber: String?,
    val partNumber: String?,
    val productName: String?,
    val odometerMeters: Double?,
    val rearWheelCircumferenceMillimeters: Double?,
    val maximumAssistanceSpeedKmh: Double?,
    val walkAssistEnabled: Boolean?,
    val walkAssistMaximumSpeedKmh: Double?,
    val activeAssistModes: List<BoschAssistMode>,
    val totalPowerOnHours: Int?,
    val supportPowerOnHours: Int?,
)

data class BoschComponent(
    val serialNumber: String?,
    val partNumber: String?,
    val productName: String?,
)

data class BoschBattery(
    val serialNumber: String?,
    val partNumber: String?,
    val productName: String?,
    val deliveredWhOverLifetime: Int?,
    val totalChargeCycles: Double?,
    val onBikeChargeCycles: Double?,
    val offBikeChargeCycles: Double?,
)

data class BoschAssistMode(
    val name: String,
    val reachableRangeKm: Double?,
)
