package info.meuse24.m24bikestats.presentation.map

import info.meuse24.m24bikestats.domain.model.ActivityMapPoint
import org.json.JSONArray
import org.json.JSONObject

object ActivityMapPointGeoJsonMapper {

    fun toGeoJsonString(points: List<ActivityMapPoint>): String {
        val features = JSONArray()
        for (point in points) {
            val geometry = JSONObject().apply {
                put("type", "Point")
                put("coordinates", JSONArray().apply {
                    put(point.longitude)  // GeoJSON: [lng, lat]
                    put(point.latitude)
                })
            }
            val feature = JSONObject().apply {
                put("type", "Feature")
                put("geometry", geometry)
                put("properties", JSONObject().apply {
                    put("activityId", point.activityId)
                })
            }
            features.put(feature)
        }
        return JSONObject().apply {
            put("type", "FeatureCollection")
            put("features", features)
        }.toString()
    }
}
