package info.meuse24.m24bikestats.domain.model

data class BoschBikePass(
    val bikeId: String,
    val frameNumber: String?,
    val frameNumberPosition: String?,
    val description: String?,
    val createdAt: String?,
    val updatedAt: String?,
)

data class BoschTheftReportLog(
    val theftReportLogId: String,
    val bikeId: String?,
    val createdAt: String?,
    val expiresAtEpochMillis: Long?,
    val timeZone: String?,
    val theftCaseEnteredAt: String?,
    val riderPortalLink: String?,
    val description: String?,
    val location: BoschTheftLocation?,
)

data class BoschTheftLocation(
    val detectedAt: String?,
    val latitude: Double?,
    val longitude: Double?,
    val horizontalAccuracyMeters: Double?,
    val address: String?,
    val description: String?,
)
