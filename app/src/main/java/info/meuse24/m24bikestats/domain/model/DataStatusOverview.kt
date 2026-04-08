package info.meuse24.m24bikestats.domain.model

data class DataStatusOverview(
    val cachedActivityCount: Int,
    val coveredActivityStartEpochMillis: Long?,
    val coveredActivityEndEpochMillis: Long?,
    val detailedActivityCount: Int,
    val missingDetailCount: Int,
    val staleDetailCount: Int,
    val gpsPointCount: Int,
    val lastActivitySyncAtEpochMillis: Long?,
    val lastBikeSyncAtEpochMillis: Long?,
    val lastDetailSyncAtEpochMillis: Long?,
    val status: DataStatusState,
)

enum class DataStatusState {
    EMPTY,
    PARTIAL,
    COMPLETE,
}
