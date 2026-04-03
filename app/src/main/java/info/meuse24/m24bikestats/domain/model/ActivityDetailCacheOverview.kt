package info.meuse24.m24bikestats.domain.model

data class ActivityDetailCacheOverview(
    val detailedActivityCount: Int,
    val detailPointCount: Int,
    val gpsPointCount: Int,
)

data class ActivityDetailCacheMetadata(
    val activityId: String,
    val pointCount: Int,
    val gpsPointCount: Int,
    val updatedAtEpochMillis: Long,
)
