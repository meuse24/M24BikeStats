package info.meuse24.m24bikestats.data.local.mapper

import info.meuse24.m24bikestats.data.local.entity.ActivityDetailEntity
import info.meuse24.m24bikestats.data.local.entity.ActivityDetailPointEntity
import info.meuse24.m24bikestats.data.local.model.CachedActivityDetail
import info.meuse24.m24bikestats.domain.model.BoschActivityDetail
import info.meuse24.m24bikestats.domain.model.BoschActivityDetailPoint

fun CachedActivityDetail.toDomain(): BoschActivityDetail =
    BoschActivityDetail(
        activityId = detail.activityId,
        points = points
            .sortedBy { it.pointIndex }
            .map { it.toDomain() },
    )

fun BoschActivityDetail.toEntity(): ActivityDetailEntity =
    ActivityDetailEntity(
        activityId = activityId,
        pointCount = points.size,
        gpsPointCount = points.count { point ->
            val latitude = point.latitude
            val longitude = point.longitude
            latitude != null && longitude != null && (latitude != 0.0 || longitude != 0.0)
        },
        updatedAtEpochMillis = System.currentTimeMillis(),
    )

fun BoschActivityDetail.toPointEntities(): List<ActivityDetailPointEntity> =
    points.mapIndexed { index, point ->
        ActivityDetailPointEntity(
            activityId = activityId,
            pointIndex = index,
            distanceMeters = point.distanceMeters,
            altitudeMeters = point.altitudeMeters,
            speedKmh = point.speedKmh,
            cadenceRpm = point.cadenceRpm,
            latitude = point.latitude,
            longitude = point.longitude,
            riderPowerWatts = point.riderPowerWatts,
        )
    }

private fun ActivityDetailPointEntity.toDomain(): BoschActivityDetailPoint =
    BoschActivityDetailPoint(
        distanceMeters = distanceMeters,
        altitudeMeters = altitudeMeters,
        speedKmh = speedKmh,
        cadenceRpm = cadenceRpm,
        latitude = latitude,
        longitude = longitude,
        riderPowerWatts = riderPowerWatts,
    )
