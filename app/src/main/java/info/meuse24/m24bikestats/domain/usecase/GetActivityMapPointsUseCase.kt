package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.model.ActivityMapPoint
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetActivityMapPointsUseCase(
    private val repository: BoschSmartSystemRepository,
) {
    operator fun invoke(): Flow<List<ActivityMapPoint>> =
        repository.observeCachedActivities().map { activities ->
            activities.mapNotNull { activity ->
                val lat = activity.centerLatitude ?: return@mapNotNull null
                val lng = activity.centerLongitude ?: return@mapNotNull null
                ActivityMapPoint(activity.id, lat, lng)
            }
        }
}
