package info.meuse24.m24bikestats.presentation.map

import info.meuse24.m24bikestats.domain.model.ActivityMapPoint
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityMapPointGeoJsonMapperTest {

    @Test
    fun `toGeoJsonString generates valid FeatureCollection`() {
        val points = listOf(ActivityMapPoint("1", 51.0, 11.0))
        val json = JSONObject(ActivityMapPointGeoJsonMapper.toGeoJsonString(points))
        
        assertEquals("FeatureCollection", json.getString("type"))
        val features = json.getJSONArray("features")
        assertEquals(1, features.length())
        
        val feature = features.getJSONObject(0)
        val geometry = feature.getJSONObject("geometry")
        val coords = geometry.getJSONArray("coordinates")
        
        assertEquals(11.0, coords.getDouble(0), 0.0) // longitude
        assertEquals(51.0, coords.getDouble(1), 0.0) // latitude
        assertEquals("1", feature.getJSONObject("properties").getString("activityId"))
    }

    @Test
    fun `toGeoJsonString returns empty FeatureCollection for empty list`() {
        val json = JSONObject(ActivityMapPointGeoJsonMapper.toGeoJsonString(emptyList()))
        assertEquals(0, json.getJSONArray("features").length())
    }
}
