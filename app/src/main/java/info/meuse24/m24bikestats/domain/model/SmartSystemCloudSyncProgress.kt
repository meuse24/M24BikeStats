package info.meuse24.m24bikestats.domain.model

data class SmartSystemCloudSyncProgress(
    val phase: SmartSystemCloudSyncPhase,
    val processedCount: Int,
    val totalCount: Int,
)

enum class SmartSystemCloudSyncPhase {
    BIKES,
    ACTIVITIES,
    ACTIVITY_DETAILS,
}
