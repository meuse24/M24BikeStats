package info.meuse24.m24bikestats.presentation.apitest

import info.meuse24.m24bikestats.support.apitest.BoschEndpoint
import info.meuse24.m24bikestats.shared.TokenInfoFormat
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed interface BoschReadableResult {
    data class Activities(
        val total: Int,
        val items: List<ActivityItem>,
    ) : BoschReadableResult

    data class BikeList(
        val bikes: List<BikeItem>,
    ) : BoschReadableResult

    data class BikeDetail(
        val bike: BikeItem,
    ) : BoschReadableResult

    data class UserInfo(
        val email: String,
        val username: String,
        val subject: String,
    ) : BoschReadableResult

    data class TokenInfo(
        val audience: List<String>,
        val scope: String,
        val boschId: String?,
        val riderId: String?,
        val expiresAt: String?,
    ) : BoschReadableResult

    data class OidcDiscovery(
        val issuer: String,
        val authorizationEndpoint: String,
        val tokenEndpoint: String,
        val userInfoEndpoint: String,
        val supportedGrants: List<String>,
    ) : BoschReadableResult
}

data class ActivityItem(
    val title: String,
    val startedAt: String,
    val duration: String,
    val distanceKm: String,
    val averageSpeed: String,
    val maxSpeed: String,
    val cadence: String?,
    val riderPower: String?,
    val elevation: String?,
    val calories: String?,
)

data class BikeItem(
    val id: String,
    val driveUnitName: String,
    val headUnitName: String?,
    val remoteName: String?,
    val odometerKm: String?,
    val assistSpeedKmh: String?,
    val batterySummary: List<String>,
    val assistModes: List<String>,
)

fun parseReadableResult(endpoint: BoschEndpoint, response: String): BoschReadableResult? {
    return when (endpoint) {
        BoschEndpoint.SMART_ACTIVITIES -> parseActivities(response)
        BoschEndpoint.SMART_BIKES -> parseBikeList(response)
        BoschEndpoint.SMART_BIKE_DETAIL -> parseBikeDetail(response)
        BoschEndpoint.USERINFO -> parseUserInfo(response)
        BoschEndpoint.TOKEN_INFO -> parseTokenInfo(response)
        BoschEndpoint.OIDC_DISCOVERY -> parseOidcDiscovery(response)
        else -> null
    }
}

private fun parseActivities(response: String): BoschReadableResult.Activities? {
    val json = extractJsonBody(response) ?: return null
    val root = JSONObject(json)
    val items = root.optJSONArray("activitySummaries") ?: return null
    return BoschReadableResult.Activities(
        total = root.optJSONObject("pagination")?.optInt("total") ?: items.length(),
        items = items.mapObjects { activity ->
            ActivityItem(
                title = activity.optString("title").ifBlank { "Aktivität" },
                startedAt = activity.optString("startTime").toReadableDateTime(),
                duration = activity.optInt("durationWithoutStops").toDurationText(),
                distanceKm = activity.optInt("distance").toKilometerText(),
                averageSpeed = activity.optJSONObject("speed")
                    ?.optMetricText("average", unit = "km/h")
                    ?: "n/a",
                maxSpeed = activity.optJSONObject("speed")
                    ?.optMetricText("maximum", unit = "km/h")
                    ?: "n/a",
                cadence = activity.optJSONObject("cadence")?.let {
                    val average = it.optMetricText("average", unit = "rpm")
                    val maximum = it.optMetricText("maximum", unit = "rpm")
                    if (average == null && maximum == null) {
                        null
                    } else {
                        listOfNotNull(
                            average?.let { value -> "Ø $value" },
                            maximum?.let { value -> "max $value" },
                        ).joinToString(", ")
                    }
                },
                riderPower = activity.optJSONObject("riderPower")?.let {
                    val average = it.optMetricText("average", unit = "W")
                    val maximum = it.optMetricText("maximum", unit = "W")
                    if (average == null && maximum == null) {
                        null
                    } else {
                        listOfNotNull(
                            average?.let { value -> "Ø $value" },
                            maximum?.let { value -> "max $value" },
                        ).joinToString(", ")
                    }
                },
                elevation = activity.optJSONObject("elevation")?.let {
                    "+${it.optInt("gain")} m / -${it.optInt("loss")} m"
                },
                calories = activity.optDouble("caloriesBurned")
                    .takeIf { !it.isNaN() && it > 0.0 }
                    ?.toWholeNumber()
                    ?.let { "$it kcal" },
            )
        }
    )
}

private fun parseBikeList(response: String): BoschReadableResult.BikeList? {
    val json = extractJsonBody(response) ?: return null
    val root = JSONObject(json)
    val bikes = root.optJSONArray("bikes") ?: return null
    return BoschReadableResult.BikeList(
        bikes = bikes.mapObjects(::parseBikeItem)
    )
}

private fun parseBikeDetail(response: String): BoschReadableResult.BikeDetail? {
    val json = extractJsonBody(response) ?: return null
    return BoschReadableResult.BikeDetail(
        bike = parseBikeItem(JSONObject(json))
    )
}

private fun parseUserInfo(response: String): BoschReadableResult.UserInfo? {
    val json = extractJsonBody(response) ?: return null
    val root = JSONObject(json)
    return BoschReadableResult.UserInfo(
        email = root.optString("email"),
        username = root.optString("preferred_username"),
        subject = root.optString("sub"),
    )
}

private fun parseTokenInfo(response: String): BoschReadableResult.TokenInfo? {
    val payload = TokenInfoFormat.extractPayload(response) ?: return null
    val root = JSONObject(payload)
    return BoschReadableResult.TokenInfo(
        audience = root.optJSONArray("aud")?.toStringList().orEmpty(),
        scope = root.optString("scope"),
        boschId = root.optString("bosch-id").ifBlank { null },
        riderId = root.optString("ebike-rider-id").ifBlank { null },
        expiresAt = root.optLong("exp")
            .takeIf { it > 0L }
            ?.let { Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault()).format(DATE_TIME_FORMATTER) },
    )
}

private fun parseOidcDiscovery(response: String): BoschReadableResult.OidcDiscovery? {
    val json = extractJsonBody(response) ?: return null
    val root = JSONObject(json)
    return BoschReadableResult.OidcDiscovery(
        issuer = root.optString("issuer"),
        authorizationEndpoint = root.optString("authorization_endpoint"),
        tokenEndpoint = root.optString("token_endpoint"),
        userInfoEndpoint = root.optString("userinfo_endpoint"),
        supportedGrants = root.optJSONArray("grant_types_supported")?.toStringList().orEmpty(),
    )
}

private fun parseBikeItem(root: JSONObject): BikeItem {
    val driveUnit = root.optJSONObject("driveUnit")
    val batteries = root.optJSONArray("batteries")
    return BikeItem(
        id = root.optString("id"),
        driveUnitName = driveUnit?.optString("productName").orEmpty().ifBlank { "Bike" },
        headUnitName = root.optJSONObject("headUnit")?.optString("productName")?.ifBlank { null },
        remoteName = root.optJSONObject("remoteControl")?.optString("productName")?.ifBlank { null },
        odometerKm = driveUnit?.optDouble("odometer")
            ?.takeIf { !it.isNaN() && it > 0.0 }
            ?.div(1000.0)
            ?.let { String.format(Locale.US, "%.1f km", it) },
        assistSpeedKmh = driveUnit?.optDouble("maximumAssistanceSpeed")
            ?.takeIf { !it.isNaN() && it > 0.0 }
            ?.let { String.format(Locale.US, "%.1f km/h", it) },
        batterySummary = batteries?.mapObjects { battery ->
            buildString {
                append(battery.optString("productName").ifBlank { "Batterie" })
                val delivered = battery.optInt("deliveredWhOverLifetime")
                if (delivered > 0) append(" • geliefert ${delivered} Wh")
                val cycles = battery.optJSONObject("chargeCycles")?.optDouble("total")
                if (cycles != null && !cycles.isNaN()) append(" • Zyklen ${String.format(Locale.US, "%.1f", cycles)}")
            }
        }.orEmpty(),
        assistModes = driveUnit?.optJSONArray("activeAssistModes")?.mapObjects { mode ->
            val name = mode.optString("name").ifBlank { "Modus" }
            val range = mode.optDouble("reachableRange")
            if (range.isNaN() || range <= 0.0) name else "$name • ${range.toWholeNumber()} km"
        }.orEmpty(),
    )
}

internal fun extractJsonBody(response: String?): String? {
    if (response.isNullOrBlank()) return null
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

private fun JSONArray.toStringList(): List<String> {
    val values = mutableListOf<String>()
    for (index in 0 until length()) {
        values += optString(index)
    }
    return values.filter { it.isNotBlank() }
}

private fun String.toReadableDateTime(): String {
    return runCatching {
        Instant.parse(this)
            .atZone(ZoneId.systemDefault())
            .format(DATE_TIME_FORMATTER)
    }.getOrDefault(this)
}

private fun Int.toDurationText(): String {
    val hours = this / 3600
    val minutes = (this % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes} min"
    }
}

private fun Int.toKilometerText(): String =
    String.format(Locale.US, "%.1f km", this / 1000.0)

private fun Double.toWholeNumber(): String =
    String.format(Locale.US, "%.0f", this)

private fun JSONObject.optMetricText(key: String, unit: String): String? {
    val value = optDouble(key)
    return value.takeIf { !it.isNaN() }?.let {
        if (unit == "km/h") String.format(Locale.US, "%.1f %s", it, unit)
        else "${it.toWholeNumber()} $unit"
    }
}

private val DATE_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
