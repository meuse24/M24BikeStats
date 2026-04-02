package info.meuse24.m24bikestats.data.repository

import info.meuse24.m24bikestats.data.remote.BoschApiClient
import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.model.BoschActivityPage
import info.meuse24.m24bikestats.domain.model.BoschAssistMode
import info.meuse24.m24bikestats.domain.model.BoschBattery
import info.meuse24.m24bikestats.domain.model.BoschBike
import info.meuse24.m24bikestats.domain.model.BoschRequest
import info.meuse24.m24bikestats.domain.model.BoschComponent
import info.meuse24.m24bikestats.domain.model.BoschDriveUnit
import info.meuse24.m24bikestats.domain.model.BoschEndpoint
import info.meuse24.m24bikestats.domain.repository.BoschSmartSystemRepository
import org.json.JSONArray
import org.json.JSONObject

class BoschSmartSystemRepositoryImpl(
    private val apiClient: BoschApiClient,
) : BoschSmartSystemRepository {

    override suspend fun getActivities(
        accessToken: String,
        limit: Int,
        offset: Int,
    ): Result<BoschActivityPage> =
        runCatching {
            val response = apiClient.get(
                BoschRequest(
                    label = BoschEndpoint.SMART_ACTIVITIES.label,
                    baseUrl = BoschEndpoint.SMART_ACTIVITIES.baseUrl,
                    path = "/activity/smart-system/v1/activities?limit=$limit&offset=$offset",
                ),
                accessToken
            )
            val json = extractJsonBody(response) ?: error("Keine Aktivitätendaten erhalten")
            val root = JSONObject(json)
            val items = root.optJSONArray("activitySummaries") ?: JSONArray()
            val pagination = root.optJSONObject("pagination")
            BoschActivityPage(
                total = pagination?.optInt("total") ?: items.length(),
                offset = pagination?.optInt("offset") ?: offset,
                limit = pagination?.optInt("limit") ?: limit,
                items = items.mapObjects(::parseActivity),
            )
        }

    override suspend fun getBikes(accessToken: String): Result<List<BoschBike>> =
        runCatching {
            val response = apiClient.get(BoschEndpoint.SMART_BIKES.toRequest(), accessToken)
            val json = extractJsonBody(response) ?: error("Keine Bike-Daten erhalten")
            val root = JSONObject(json)
            val items = root.optJSONArray("bikes") ?: JSONArray()
            items.mapObjects(::parseBike)
        }

    override suspend fun getBikeDetail(accessToken: String, bikeId: String): Result<BoschBike> =
        runCatching {
            val response = apiClient.get(
                BoschEndpoint.SMART_BIKE_DETAIL.toRequest(bikeId = bikeId),
                accessToken
            )
            val json = extractJsonBody(response) ?: error("Keine Bike-Detaildaten erhalten")
            parseBike(JSONObject(json))
        }

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

    private fun parseComponent(root: JSONObject): BoschComponent {
        return BoschComponent(
            serialNumber = root.optString("serialNumber").ifBlank { null },
            partNumber = root.optString("partNumber").ifBlank { null },
            productName = root.optString("productName").ifBlank { null },
        )
    }

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

    private fun parseAssistMode(root: JSONObject): BoschAssistMode {
        return BoschAssistMode(
            name = root.optString("name").ifBlank { "Modus" },
            reachableRangeKm = root.optNullableDouble("reachableRange"),
        )
    }

    private fun extractJsonBody(response: String): String? {
        return response.substringAfter("\n\n", missingDelimiterValue = response)
            .trim()
            .takeIf { it.startsWith("{") || it.startsWith("[") }
    }

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
