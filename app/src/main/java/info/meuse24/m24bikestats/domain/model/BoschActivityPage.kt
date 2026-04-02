package info.meuse24.m24bikestats.domain.model

data class BoschActivityPage(
    val total: Int,
    val offset: Int,
    val limit: Int,
    val items: List<BoschActivity>,
)
