package info.meuse24.m24bikestats.data.remote

import info.meuse24.m24bikestats.domain.model.BoschActivity
import info.meuse24.m24bikestats.domain.model.BoschActivityDetail
import info.meuse24.m24bikestats.domain.model.BoschActivityDetailPoint
import info.meuse24.m24bikestats.domain.model.BoschActivityPage
import info.meuse24.m24bikestats.domain.model.BoschAssistMode
import info.meuse24.m24bikestats.domain.model.BoschBattery
import info.meuse24.m24bikestats.domain.model.BoschBike
import info.meuse24.m24bikestats.domain.model.BoschBikePass
import info.meuse24.m24bikestats.domain.model.BoschRegistration
import info.meuse24.m24bikestats.domain.model.BoschServiceRecord
import info.meuse24.m24bikestats.domain.model.BoschSoftwareUpdateSummary
import info.meuse24.m24bikestats.domain.model.BoschBatteryMeasurement
import info.meuse24.m24bikestats.domain.model.BoschComponent
import info.meuse24.m24bikestats.domain.model.BoschDriveUnit
import info.meuse24.m24bikestats.domain.model.BoschTheftLocation
import info.meuse24.m24bikestats.domain.model.BoschTheftReportLog
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

    internal fun parseBikePassData(json: String, bikeId: String): BoschBikePassData {
        val root = JSONObject(json)
        val bikePass = root.optJSONArray("bikePasses")
            ?.mapObjects(::parseBikePass)
            ?.firstOrNull { it.bikeId == bikeId }
        val theftReportLogs = root.optJSONArray("theftReportLogs")
            ?.mapObjects(::parseTheftReportLog)
            ?.filter { it.bikeId == null || it.bikeId == bikeId }
            .orEmpty()
        return BoschBikePassData(
            bikePass = bikePass,
            theftReportLogs = theftReportLogs,
        )
    }

    fun parseServiceRecords(json: String, bikeId: String): List<BoschServiceRecord> {
        val root = JSONObject(json)
        return root.optJSONArray("serviceRecords")
            ?.mapObjects(::parseServiceRecord)
            ?.filter { it.bikeId == bikeId }
            .orEmpty()
    }

    fun parseRegistrations(json: String): List<BoschRegistration> {
        val root = JSONObject(json)
        return root.optJSONArray("registrations")
            ?.mapObjects(::parseRegistration)
            .orEmpty()
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
        val serviceDue = root.optJSONObject("serviceDue")
        return BoschBike(
            id = root.optString("id"),
            createdAt = root.optString("createdAt").ifBlank { null },
            language = root.optString("language").ifBlank { null },
            oemId = root.optString("oemId").ifBlank { null },
            serviceDueDate = serviceDue?.optString("date")?.ifBlank { null },
            serviceDueOdometerMeters = serviceDue.optNullableDouble("odometer"),
            driveUnit = driveUnit?.let { parseDriveUnit(it) },
            remoteControl = root.optJSONObject("remoteControl")?.let(::parseComponent),
            headUnit = root.optJSONObject("headUnit")?.let(::parseComponent),
            connectModule = root.optJSONObject("connectModule")?.let(::parseComponent),
            antiLockBrakeSystems = root.optJSONArray("antiLockBrakeSystems")?.mapObjects(::parseComponent).orEmpty(),
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

    private fun parseBikePass(root: JSONObject): BoschBikePass =
        BoschBikePass(
            bikeId = root.optString("bikeId"),
            frameNumber = root.optString("frameNumber").ifBlank { null },
            frameNumberPosition = root.optString("frameNumberPosition").ifBlank { null },
            description = root.optString("description").ifBlank { null },
            createdAt = root.optString("createdAt").ifBlank { null },
            updatedAt = root.optString("updatedAt").ifBlank { null },
        )

    private fun parseTheftReportLog(root: JSONObject): BoschTheftReportLog =
        BoschTheftReportLog(
            theftReportLogId = root.optString("theftReportLogId"),
            bikeId = root.optString("bikeId").ifBlank { null },
            createdAt = root.optString("createdAt").ifBlank { null },
            expiresAtEpochMillis = root.optLong("expiresAt").takeIf { it > 0L },
            timeZone = root.optString("timeZone").ifBlank { null },
            theftCaseEnteredAt = root.optString("theftCaseEnteredAt").ifBlank { null },
            riderPortalLink = root.optString("riderPortalLink").ifBlank { null },
            description = root.optString("description").ifBlank { null },
            location = root.optJSONObject("location")?.let(::parseTheftLocation),
        )

    private fun parseTheftLocation(root: JSONObject): BoschTheftLocation =
        BoschTheftLocation(
            detectedAt = root.optString("detectedAt").ifBlank { null },
            latitude = root.optNullableDouble("latitude"),
            longitude = root.optNullableDouble("longitude"),
            horizontalAccuracyMeters = root.optNullableDouble("horizontalAccuracy"),
            address = root.optString("address").ifBlank { null },
            description = root.optString("description").ifBlank { null },
        )

    private fun parseServiceRecord(root: JSONObject): BoschServiceRecord {
        val attributes = root.optJSONObject("attributes") ?: JSONObject()
        val details = attributes.optJSONObject("details") ?: JSONObject()
        val bikeDealer = attributes.optJSONObject("bikeDealer")
        val batteryMeasurement = details.optJSONObject("batteryMeasurement")
        val measurement = batteryMeasurement?.optJSONObject("measurement")
        val softwareUpdate = details.optJSONObject("softwareUpdate")
        val softwareUpdateClient = softwareUpdate?.optJSONObject("client")
        val softwareUpdateBike = softwareUpdate?.optJSONObject("bike")
        val updatedComponents = softwareUpdateBike?.optJSONArray("updatedComponents")
        return BoschServiceRecord(
            id = root.optString("id"),
            type = root.optString("type").ifBlank { "UNKNOWN" },
            bikeId = attributes.optString("bikeId"),
            createdAt = attributes.optString("createdAt"),
            odometerValueMeters = attributes.optLong("odometerValue").takeIf { it > 0L },
            bikeDealerName = bikeDealer?.optString("name")?.ifBlank { null },
            bikeDealerCity = bikeDealer?.optString("city")?.ifBlank { null },
            toolVersion = details.optString("toolVersion").ifBlank { null },
            batteryMeasurement = measurement?.let {
                BoschBatteryMeasurement(
                    fullChargeCycles = it.optInt("fullChargeCycles").takeIf { value -> value >= 0 },
                    measuredEnergyCapacityWh = it.optInt("measuredEnergyCapacity").takeIf { value -> value >= 0 },
                    nominalEnergyCapacityWh = it.optInt("nominalEnergyCapacity").takeIf { value -> value >= 0 },
                    measuredCapacityPercentage = it.optInt("measuredCapacityPercentage").takeIf { value -> value >= 0 },
                    onBikeMeasurement = if (it.has("onBikeMeasurement")) it.optBoolean("onBikeMeasurement") else null,
                )
            },
            softwareUpdate = softwareUpdate?.let {
                BoschSoftwareUpdateSummary(
                    clientType = softwareUpdateClient?.optString("type")?.ifBlank { null },
                    clientVersion = softwareUpdateClient?.optString("version")?.ifBlank { null },
                    isForcedUpdate = if (it.has("isForcedUpdate")) it.optBoolean("isForcedUpdate") else null,
                    updatedComponentsCount = updatedComponents?.length() ?: 0,
                    updatedComponentNames = updatedComponents
                        ?.mapNotNullObjects { component ->
                            component.optString("productName").ifBlank {
                                component.optString("productCode").ifBlank { null }
                            }
                        }
                        .orEmpty(),
                )
            },
        )
    }

    private fun parseRegistration(root: JSONObject): BoschRegistration {
        val bikeRegistration = root.optJSONObject("bikeRegistration")
        val componentRegistration = root.optJSONObject("componentRegistration")
        return BoschRegistration(
            registrationType = root.optString("registrationType").ifBlank { "UNKNOWN" },
            createdAt = root.optString("createdAt"),
            bikeId = bikeRegistration?.optString("bikeId")?.ifBlank { null },
            componentType = componentRegistration?.optString("componentType")?.ifBlank { null },
            partNumber = componentRegistration?.optString("partNumber")?.ifBlank { null },
            serialNumber = componentRegistration?.optString("serialNumber")?.ifBlank { null },
        )
    }

    private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> {
        val result = mutableListOf<T>()
        for (index in 0 until length()) {
            optJSONObject(index)?.let { result += transform(it) }
        }
        return result
    }

    private fun <T> JSONArray.mapNotNullObjects(transform: (JSONObject) -> T?): List<T> {
        val result = mutableListOf<T>()
        for (index in 0 until length()) {
            optJSONObject(index)?.let { item ->
                transform(item)?.let { result += it }
            }
        }
        return result
    }

    private fun JSONObject?.optNullableDouble(key: String): Double? {
        val value = this?.optDouble(key) ?: return null
        return value.takeIf { !it.isNaN() }
    }
}

internal data class BoschBikePassData(
    val bikePass: BoschBikePass?,
    val theftReportLogs: List<BoschTheftReportLog>,
)
