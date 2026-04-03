package info.meuse24.m24bikestats.data.local.mapper

import info.meuse24.m24bikestats.data.local.entity.ActivityEntity
import info.meuse24.m24bikestats.domain.model.BoschActivity

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
    )

fun BoschActivity.toEntity(): ActivityEntity =
    ActivityEntity(
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
    )
