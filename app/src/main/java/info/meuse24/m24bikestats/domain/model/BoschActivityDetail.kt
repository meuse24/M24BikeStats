package info.meuse24.m24bikestats.domain.model

data class BoschActivityDetail(
    val activityId: String,
    val points: List<BoschActivityDetailPoint>,
)

data class BoschActivityDetailPoint(
    val distanceMeters: Double?,
    val altitudeMeters: Double?,
    val speedKmh: Double?,
    val cadenceRpm: Double?,
    val latitude: Double?,
    val longitude: Double?,
    val riderPowerWatts: Double?,
)
