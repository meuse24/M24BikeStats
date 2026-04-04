package info.meuse24.m24bikestats.presentation.apitest

import info.meuse24.m24bikestats.api.BoschEndpoint

data class ApiTestResponseDiagnostics(
    val unusedFieldLines: List<String>,
)

internal fun buildApiTestResponseDiagnostics(
    endpoint: BoschEndpoint,
    response: String,
): ApiTestResponseDiagnostics? {
    val unusedFieldLines = buildUnusedFieldLines(endpoint, response)
    if (unusedFieldLines.isEmpty()) return null
    return ApiTestResponseDiagnostics(
        unusedFieldLines = unusedFieldLines,
    )
}

private data class FieldValue(
    val path: String,
    val preview: String,
)

private sealed interface JsonNode {
    data class JsonObject(val properties: Map<String, JsonNode>) : JsonNode
    data class JsonArray(val items: List<JsonNode>) : JsonNode
    data class JsonString(val value: String) : JsonNode
    data class JsonNumber(val raw: String) : JsonNode
    data class JsonBoolean(val value: Boolean) : JsonNode
    data object JsonNull : JsonNode
}

private fun buildUnusedFieldLines(
    endpoint: BoschEndpoint,
    response: String,
): List<String> {
    val json = extractJsonBody(response) ?: return emptyList()
    val root = parseJsonRoot(json) ?: return emptyList()
    val usedPaths = knownUsedPaths(endpoint)
    if (usedPaths.isEmpty()) return emptyList()

    return collectFieldValues(root)
        .filterNot { field -> field.path in usedPaths }
        .sortedBy(FieldValue::path)
        .take(MAX_UNUSED_FIELDS)
        .map { field -> "${field.path} = ${field.preview}" }
}

private fun parseJsonRoot(json: String): Any? = runCatching {
    JsonParser(json).parse()
}.getOrNull()

private fun collectFieldValues(root: Any): List<FieldValue> {
    val values = mutableListOf<FieldValue>()

    fun visit(node: Any?, path: String) {
        when (node) {
            is JsonNode.JsonObject -> {
                if (node.properties.isEmpty() && path.isNotBlank()) {
                    values += FieldValue(path = path, preview = "{}")
                }
                node.properties.keys.sorted().forEach { key ->
                    val childPath = if (path.isBlank()) key else "$path.$key"
                    visit(node.properties.getValue(key), childPath)
                }
            }

            is JsonNode.JsonArray -> {
                val arrayPath = if (path.isBlank()) "[]" else "$path[]"
                if (node.items.isEmpty()) {
                    values += FieldValue(path = arrayPath, preview = "[]")
                }
                node.items.forEach { item ->
                    visit(item, arrayPath)
                }
            }

            JsonNode.JsonNull,
            null,
            -> if (path.isNotBlank()) {
                values += FieldValue(path = path, preview = "null")
            }

            is JsonNode.JsonString -> if (path.isNotBlank()) {
                values += FieldValue(path = path, preview = node.value.toPreview())
            }

            is JsonNode.JsonNumber -> if (path.isNotBlank()) {
                values += FieldValue(path = path, preview = node.raw)
            }

            is JsonNode.JsonBoolean -> if (path.isNotBlank()) {
                values += FieldValue(path = path, preview = node.value.toString())
            }
        }
    }

    visit(root, path = "")
    return values.distinctBy { "${it.path}=${it.preview}" }
}

private fun String.toPreview(): String =
    "\"${replace("\n", "\\n").take(MAX_PREVIEW_LENGTH)}\""

private fun knownUsedPaths(endpoint: BoschEndpoint): Set<String> =
    when (endpoint) {
        BoschEndpoint.SMART_ACTIVITIES -> setOf(
            "activitySummaries[].id",
            "activitySummaries[].title",
            "activitySummaries[].startTime",
            "activitySummaries[].endTime",
            "activitySummaries[].timeZone",
            "activitySummaries[].durationWithoutStops",
            "activitySummaries[].bikeId",
            "activitySummaries[].startOdometer",
            "activitySummaries[].distance",
            "activitySummaries[].speed.average",
            "activitySummaries[].speed.maximum",
            "activitySummaries[].cadence.average",
            "activitySummaries[].cadence.maximum",
            "activitySummaries[].riderPower.average",
            "activitySummaries[].riderPower.maximum",
            "activitySummaries[].elevation.gain",
            "activitySummaries[].elevation.loss",
            "activitySummaries[].caloriesBurned",
            "pagination.total",
            "pagination.offset",
            "pagination.limit",
            "links.prev",
            "links.next",
            "links.last",
            "links.self",
            "links.first",
        )

        BoschEndpoint.SMART_ACTIVITY_DETAIL -> setOf(
            "activityDetails[].distance",
            "activityDetails[].altitude",
            "activityDetails[].speed",
            "activityDetails[].cadence",
            "activityDetails[].latitude",
            "activityDetails[].longitude",
            "activityDetails[].riderPower",
        )

        BoschEndpoint.SMART_BIKES,
        BoschEndpoint.SMART_BIKE_DETAIL,
        -> setOf(
            "bikes[].id",
            "bikes[].createdAt",
            "bikes[].language",
            "bikes[].driveUnit.serialNumber",
            "bikes[].driveUnit.partNumber",
            "bikes[].driveUnit.productName",
            "bikes[].driveUnit.walkAssistConfiguration.isEnabled",
            "bikes[].driveUnit.walkAssistConfiguration.maximumSpeed",
            "bikes[].driveUnit.odometer",
            "bikes[].driveUnit.rearWheelCircumferenceUser",
            "bikes[].driveUnit.maximumAssistanceSpeed",
            "bikes[].driveUnit.activeAssistModes[].name",
            "bikes[].driveUnit.activeAssistModes[].reachableRange",
            "bikes[].driveUnit.powerOnTime.total",
            "bikes[].driveUnit.powerOnTime.withMotorSupport",
            "bikes[].remoteControl.serialNumber",
            "bikes[].remoteControl.partNumber",
            "bikes[].remoteControl.productName",
            "bikes[].headUnit.serialNumber",
            "bikes[].headUnit.partNumber",
            "bikes[].headUnit.productName",
            "bikes[].batteries[].serialNumber",
            "bikes[].batteries[].partNumber",
            "bikes[].batteries[].productName",
            "bikes[].batteries[].deliveredWhOverLifetime",
            "bikes[].batteries[].chargeCycles.total",
            "bikes[].batteries[].chargeCycles.onBike",
            "bikes[].batteries[].chargeCycles.offBike",
            "id",
            "createdAt",
            "language",
            "driveUnit.serialNumber",
            "driveUnit.partNumber",
            "driveUnit.productName",
            "driveUnit.walkAssistConfiguration.isEnabled",
            "driveUnit.walkAssistConfiguration.maximumSpeed",
            "driveUnit.odometer",
            "driveUnit.rearWheelCircumferenceUser",
            "driveUnit.maximumAssistanceSpeed",
            "driveUnit.activeAssistModes[].name",
            "driveUnit.activeAssistModes[].reachableRange",
            "driveUnit.powerOnTime.total",
            "driveUnit.powerOnTime.withMotorSupport",
            "remoteControl.serialNumber",
            "remoteControl.partNumber",
            "remoteControl.productName",
            "headUnit.serialNumber",
            "headUnit.partNumber",
            "headUnit.productName",
            "batteries[].serialNumber",
            "batteries[].partNumber",
            "batteries[].productName",
            "batteries[].deliveredWhOverLifetime",
            "batteries[].chargeCycles.total",
            "batteries[].chargeCycles.onBike",
            "batteries[].chargeCycles.offBike",
        )

        BoschEndpoint.USERINFO -> setOf("email", "preferred_username", "sub")
        BoschEndpoint.OIDC_DISCOVERY -> setOf(
            "issuer",
            "authorization_endpoint",
            "token_endpoint",
            "userinfo_endpoint",
            "grant_types_supported[]",
        )

        else -> emptySet()
    }

private const val MAX_UNUSED_FIELDS = 12
private const val MAX_PREVIEW_LENGTH = 60

private class JsonParser(
    private val source: String,
) {
    private var index: Int = 0

    fun parse(): JsonNode? {
        skipWhitespace()
        val result = parseValue()
        skipWhitespace()
        return result.takeIf { index <= source.length }
    }

    private fun parseValue(): JsonNode? =
        when (source.getOrNull(index)) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()?.let(JsonNode::JsonString)
            't' -> parseLiteral("true")?.let { JsonNode.JsonBoolean(true) }
            'f' -> parseLiteral("false")?.let { JsonNode.JsonBoolean(false) }
            'n' -> parseLiteral("null")?.let { JsonNode.JsonNull }
            '-', in '0'..'9' -> parseNumber()?.let(JsonNode::JsonNumber)
            else -> null
        }

    private fun parseObject(): JsonNode.JsonObject? {
        if (!consume('{')) return null
        skipWhitespace()

        val properties = linkedMapOf<String, JsonNode>()
        if (consume('}')) return JsonNode.JsonObject(properties)

        while (index < source.length) {
            skipWhitespace()
            val key = parseString() ?: return null
            skipWhitespace()
            if (!consume(':')) return null
            skipWhitespace()
            val value = parseValue() ?: return null
            properties[key] = value
            skipWhitespace()
            when {
                consume('}') -> return JsonNode.JsonObject(properties)
                consume(',') -> continue
                else -> return null
            }
        }
        return null
    }

    private fun parseArray(): JsonNode.JsonArray? {
        if (!consume('[')) return null
        skipWhitespace()

        val items = mutableListOf<JsonNode>()
        if (consume(']')) return JsonNode.JsonArray(items)

        while (index < source.length) {
            skipWhitespace()
            items += parseValue() ?: return null
            skipWhitespace()
            when {
                consume(']') -> return JsonNode.JsonArray(items)
                consume(',') -> continue
                else -> return null
            }
        }
        return null
    }

    private fun parseString(): String? {
        if (!consume('"')) return null

        val rawValue = StringBuilder()
        var escaped = false
        while (index < source.length) {
            val current = source[index++]
            when {
                escaped -> {
                    rawValue.append('\\')
                    rawValue.append(current)
                    escaped = false
                }
                current == '\\' -> escaped = true
                current == '"' -> return rawValue.toString().decodeJsonString()
                else -> rawValue.append(current)
            }
        }
        return null
    }

    private fun parseNumber(): String? {
        val startIndex = index
        while (index < source.length && source[index] in JSON_NUMBER_CHARS) {
            index++
        }
        return source.substring(startIndex, index).takeIf { it.isNotBlank() }
    }

    private fun parseLiteral(literal: String): String? {
        if (!source.startsWith(literal, index)) return null
        index += literal.length
        return literal
    }

    private fun skipWhitespace() {
        while (index < source.length && source[index].isWhitespace()) {
            index++
        }
    }

    private fun consume(character: Char): Boolean {
        if (source.getOrNull(index) != character) return false
        index++
        return true
    }
}

private fun String.decodeJsonString(): String =
    buildString(this@decodeJsonString.length) {
        var index = 0
        while (index < this@decodeJsonString.length) {
            val current = this@decodeJsonString[index]
            if (current != '\\') {
                append(current)
                index++
                continue
            }

            when (val next = this@decodeJsonString.getOrNull(index + 1)) {
                '"', '\\', '/' -> {
                    append(next)
                    index += 2
                }
                'b' -> {
                    append('\b')
                    index += 2
                }
                'f' -> {
                    append('\u000C')
                    index += 2
                }
                'n' -> {
                    append('\n')
                    index += 2
                }
                'r' -> {
                    append('\r')
                    index += 2
                }
                't' -> {
                    append('\t')
                    index += 2
                }
                'u' -> {
                    val hex = this@decodeJsonString.substring(index + 2, index + 6)
                    append(hex.toInt(16).toChar())
                    index += 6
                }
                else -> {
                    append(current)
                    index++
                }
            }
        }
    }

private val JSON_NUMBER_CHARS = setOf('-', '+', '.', 'e', 'E') + ('0'..'9')
