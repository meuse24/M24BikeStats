package info.meuse24.m24bikestats.domain.model

data class BoschRegistration(
    val registrationType: String,
    val createdAt: String,
    val bikeId: String? = null,
    val componentType: String? = null,
    val partNumber: String? = null,
    val serialNumber: String? = null,
)
