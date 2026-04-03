package info.meuse24.m24bikestats.data.remote

import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.model.BoschActivityDetail
import info.meuse24.m24bikestats.domain.model.BoschActivityDetailPoint
import info.meuse24.m24bikestats.domain.model.BoschActivityPage
import info.meuse24.m24bikestats.domain.model.BoschAssistMode
import info.meuse24.m24bikestats.domain.model.BoschBattery
import info.meuse24.m24bikestats.domain.model.BoschBike
import info.meuse24.m24bikestats.domain.model.BoschComponent
import info.meuse24.m24bikestats.domain.model.BoschDriveUnit
import org.json.JSONArray
import org.json.JSONObject

class BoschSmartSystemParser {
    fun parseActivitiesPage(
        json: String,
        limit: Int,
        offset: Int,
    ): BoschActivityPage {
        val root = JSONObject(json)
        val items = root.optJSONArray("activitySummaries") ?: JSONArray()
        val pagination = root.optJSONObject("pagination")
        return BoschActivityPage(
            total = pagination?.optInt("total") ?: items.length(),
            offset = pagination?.optInt("offset") ?: offset,
            limit = pagination?.optInt("limit") ?: limit,
            items = items.mapObjects(::parseActivity),
        )
    }

    fun parseBikes(json: String): List<BoschBike> {
        val root = JSONObject(json)
        val items = root.optJSONArray("bikes") ?: JSONArray()
        return items.mapObjects(::parseBike)
    }

    fun parseActivityDetail(
        activityId: String,
        json: String,
    ): BoschActivityDetail {
        val root = JSONObject(json)
        val items = root.optJSONArray("activityDetails") ?: JSONArray()
        return BoschActivityDetail(
            activityId = activityId,
            points = items.mapObjects(::parseActivityDetailPoint),
        )
    }

    fun parseBikeDetail(json: String): BoschBike =
        parseBike(JSONObject(json))

    private fun parseActivity(root: JSONObject): BoschActivity {
        val speed = root.optJSONObject("speed")
        val cadence = root.optJSONObject("cadence")
        val riderPower = root.optJSONObject("riderPower")
        val elevation = root.optJSONObject("elevation")

        return BoschActivity(
            id = root.optString("id"),
            title = root.optString("title").ifBlank { "Aktivität" },
            startTime = root.optString("startTime"),
            endTime = root.optString("endTime").ifBlank { null },
            timeZone = root.optString("timeZone").ifBlank { null },
            durationWithoutStopsSeconds = root.optInt("durationWithoutStops"),
            bikeId = root.optString("bikeId").ifBlank { null },
            startOdometerMeters = root.optInt("startOdometer").takeIf { it > 0 },
            distanceMeters = root.optInt("distance"),
            averageSpeedKmh = speed.optNullableDouble("average"),
            maxSpeedKmh = speed.optNullableDouble("maximum"),
            averageCadenceRpm = cadence.optNullableDouble("average"),
            maxCadenceRpm = cadence.optNullableDouble("maximum"),
            averageRiderPowerWatts = riderPower.optNullableDouble("average"),
            maxRiderPowerWatts = riderPower.optNullableDouble("maximum"),
            elevationGainMeters = elevation?.optInt("gain")?.takeIf { it >= 0 },
            elevationLossMeters = elevation?.optInt("loss")?.takeIf { it >= 0 },
            caloriesBurned = root.optNullableDouble("caloriesBurned"),
        )
    }

    private fun parseBike(root: JSONObject): BoschBike {
        val driveUnit = root.optJSONObject("driveUnit")
        return BoschBike(
            id = root.optString("id"),
            createdAt = root.optString("createdAt").ifBlank { null },
            language = root.optString("language").ifBlank { null },
            driveUnit = driveUnit?.let { parseDriveUnit(it) },
            remoteControl = root.optJSONObject("remoteControl")?.let(::parseComponent),
            headUnit = root.optJSONObject("headUnit")?.let(::parseComponent),
            batteries = root.optJSONArray("batteries")?.mapObjects(::parseBattery).orEmpty(),
        )
    }

    private fun parseActivityDetailPoint(root: JSONObject): BoschActivityDetailPoint =
        BoschActivityDetailPoint(
            distanceMeters = root.optNullableDouble("distance"),
            altitudeMeters = root.optNullableDouble("altitude"),
            speedKmh = root.optNullableDouble("speed"),
            cadenceRpm = root.optNullableDouble("cadence"),
            latitude = root.optNullableDouble("latitude"),
            longitude = root.optNullableDouble("longitude"),
            riderPowerWatts = root.optNullableDouble("riderPower"),
        )

    private fun parseDriveUnit(root: JSONObject): BoschDriveUnit {
        val walkAssist = root.optJSONObject("walkAssistConfiguration")
        val powerOnTime = root.optJSONObject("powerOnTime")
        return BoschDriveUnit(
            serialNumber = root.optString("serialNumber").ifBlank { null },
            partNumber = root.optString("partNumber").ifBlank { null },
            productName = root.optString("productName").ifBlank { null },
            odometerMeters = root.optNullableDouble("odometer"),
            rearWheelCircumferenceMillimeters = root.optNullableDouble("rearWheelCircumferenceUser"),
            maximumAssistanceSpeedKmh = root.optNullableDouble("maximumAssistanceSpeed"),
            walkAssistEnabled = walkAssist?.optBoolean("isEnabled"),
            walkAssistMaximumSpeedKmh = walkAssist.optNullableDouble("maximumSpeed"),
            activeAssistModes = root.optJSONArray("activeAssistModes")?.mapObjects(::parseAssistMode).orEmpty(),
            totalPowerOnHours = powerOnTime?.optInt("total")?.takeIf { it >= 0 },
            supportPowerOnHours = powerOnTime?.optInt("withMotorSupport")?.takeIf { it >= 0 },
        )
    }

    private fun parseComponent(root: JSONObject): BoschComponent =
        BoschComponent(
            serialNumber = root.optString("serialNumber").ifBlank { null },
            partNumber = root.optString("partNumber").ifBlank { null },
            productName = root.optString("productName").ifBlank { null },
        )

    private fun parseBattery(root: JSONObject): BoschBattery {
        val cycles = root.optJSONObject("chargeCycles")
        return BoschBattery(
            serialNumber = root.optString("serialNumber").ifBlank { null },
            partNumber = root.optString("partNumber").ifBlank { null },
            productName = root.optString("productName").ifBlank { null },
            deliveredWhOverLifetime = root.optInt("deliveredWhOverLifetime").takeIf { it >= 0 },
            totalChargeCycles = cycles.optNullableDouble("total"),
            onBikeChargeCycles = cycles.optNullableDouble("onBike"),
            offBikeChargeCycles = cycles.optNullableDouble("offBike"),
        )
    }

    private fun parseAssistMode(root: JSONObject): BoschAssistMode =
        BoschAssistMode(
            name = root.optString("name").ifBlank { "Modus" },
            reachableRangeKm = root.optNullableDouble("reachableRange"),
        )

    private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> {
        val result = mutableListOf<T>()
        for (index in 0 until length()) {
            optJSONObject(index)?.let { result += transform(it) }
        }
        return result
    }

    private fun JSONObject?.optNullableDouble(key: String): Double? {
        val value = this?.optDouble(key) ?: return null
        return value.takeIf { !it.isNaN() }
    }
}
