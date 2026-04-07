package info.meuse24.m24bikestats.domain.model

data class BoschServiceRecord(
    val id: String,
    val type: String,
    val bikeId: String,
    val createdAt: String,
    val odometerValueMeters: Long?,
    val bikeDealerName: String?,
    val bikeDealerCity: String?,
    val toolVersion: String?,
    val batteryMeasurement: BoschBatteryMeasurement? = null,
    val softwareUpdate: BoschSoftwareUpdateSummary? = null,
)

data class BoschBatteryMeasurement(
    val fullChargeCycles: Int?,
    val measuredEnergyCapacityWh: Int?,
    val nominalEnergyCapacityWh: Int?,
    val measuredCapacityPercentage: Int?,
    val onBikeMeasurement: Boolean?,
)

data class BoschSoftwareUpdateSummary(
    val clientType: String?,
    val clientVersion: String?,
    val isForcedUpdate: Boolean?,
    val updatedComponentsCount: Int,
    val updatedComponentNames: List<String>,
)
