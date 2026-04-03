package info.meuse24.m24bikestats.domain.model

data class BoschActivityDetailsCsvExport(
    val fileName: String,
    val csvContent: String,
    val activityCount: Int,
    val detailPointCount: Int,
)
