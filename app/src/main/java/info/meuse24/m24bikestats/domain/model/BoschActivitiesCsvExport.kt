package info.meuse24.m24bikestats.domain.model

data class BoschActivitiesCsvExport(
    val fileName: String,
    val csvContent: String,
    val activityCount: Int,
)
