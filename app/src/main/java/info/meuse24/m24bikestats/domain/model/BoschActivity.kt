package info.meuse24.m24bikestats.domain.model

data class BoschActivity(
    val id: String,
    val title: String,
    val startTime: String,
    val endTime: String?,
    val timeZone: String?,
    val durationWithoutStopsSeconds: Int,
    val bikeId: String?,
    val startOdometerMeters: Int?,
    val distanceMeters: Int,
    val averageSpeedKmh: Double?,
    val maxSpeedKmh: Double?,
    val averageCadenceRpm: Double?,
    val maxCadenceRpm: Double?,
    val averageRiderPowerWatts: Double?,
    val maxRiderPowerWatts: Double?,
    val elevationGainMeters: Int?,
    val elevationLossMeters: Int?,
    val caloriesBurned: Double?,
    val centerLatitude: Double? = null,
    val centerLongitude: Double? = null,
)
