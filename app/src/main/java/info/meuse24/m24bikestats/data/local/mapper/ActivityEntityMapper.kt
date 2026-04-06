package info.meuse24.m24bikestats.data.local.mapper

import info.meuse24.m24bikestats.data.local.entity.ActivityEntity
import info.meuse24.m24bikestats.domain.model.BoschActivity
import java.time.Instant

fun ActivityEntity.toDomain(): BoschActivity =
    BoschActivity(
        id = id,
        title = title,
        startTime = startTime,
        endTime = endTime,
        timeZone = timeZone,
        durationWithoutStopsSeconds = durationWithoutStopsSeconds,
        bikeId = bikeId,
        startOdometerMeters = startOdometerMeters,
        distanceMeters = distanceMeters,
        averageSpeedKmh = averageSpeedKmh,
        maxSpeedKmh = maxSpeedKmh,
        averageCadenceRpm = averageCadenceRpm,
        maxCadenceRpm = maxCadenceRpm,
        averageRiderPowerWatts = averageRiderPowerWatts,
        maxRiderPowerWatts = maxRiderPowerWatts,
        elevationGainMeters = elevationGainMeters,
        elevationLossMeters = elevationLossMeters,
        caloriesBurned = caloriesBurned,
        centerLatitude = centerLatitude,
        centerLongitude = centerLongitude,
    )

fun BoschActivity.toEntity(): ActivityEntity =
    ActivityEntity(
        id = id,
        title = title,
        startTime = startTime,
        startTimeEpoch = startTime.toEpochMillis(),
        endTime = endTime,
        timeZone = timeZone,
        durationWithoutStopsSeconds = durationWithoutStopsSeconds,
        bikeId = bikeId,
        startOdometerMeters = startOdometerMeters,
        distanceMeters = distanceMeters,
        averageSpeedKmh = averageSpeedKmh,
        maxSpeedKmh = maxSpeedKmh,
        averageCadenceRpm = averageCadenceRpm,
        maxCadenceRpm = maxCadenceRpm,
        averageRiderPowerWatts = averageRiderPowerWatts,
        maxRiderPowerWatts = maxRiderPowerWatts,
        elevationGainMeters = elevationGainMeters,
        elevationLossMeters = elevationLossMeters,
        caloriesBurned = caloriesBurned,
        centerLatitude = centerLatitude,
        centerLongitude = centerLongitude,
    )

private fun String.toEpochMillis(): Long =
    runCatching { Instant.parse(this).toEpochMilli() }.getOrDefault(0L)
