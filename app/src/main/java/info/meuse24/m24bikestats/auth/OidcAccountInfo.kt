package info.meuse24.m24bikestats.auth

import info.meuse24.m24bikestats.api.BoschEndpoint
import info.meuse24.m24bikestats.api.FetchBoschDataUseCase

data class OidcUserInfoUiModel(
    val email: String?,
    val username: String?,
    val subject: String?,
)

interface OidcUserInfoProvider {
    suspend fun loadCurrentUserInfo(): OidcUserInfoUiModel?
}

class LiveOidcUserInfoProvider(
    private val fetchBoschData: FetchBoschDataUseCase,
) : OidcUserInfoProvider {
    override suspend fun loadCurrentUserInfo(): OidcUserInfoUiModel? {
        val response = fetchBoschData(BoschEndpoint.USERINFO).getOrNull() ?: return null
        return parseOidcUserInfo(response)
    }
}

data class OidcDiscoveryInfoUiModel(
    val issuer: String?,
    val authorizationEndpoint: String?,
    val tokenEndpoint: String?,
    val userInfoEndpoint: String?,
    val jwksUri: String?,
    val revocationEndpoint: String?,
    val introspectionEndpoint: String?,
    val endSessionEndpoint: String?,
    val supportedGrantTypes: List<String>,
)

interface OidcDiscoveryInfoProvider {
    suspend fun loadCurrentDiscovery(): OidcDiscoveryInfoUiModel?
}

class LiveOidcDiscoveryInfoProvider(
    private val fetchBoschData: FetchBoschDataUseCase,
) : OidcDiscoveryInfoProvider {
    override suspend fun loadCurrentDiscovery(): OidcDiscoveryInfoUiModel? {
        val response = fetchBoschData(BoschEndpoint.OIDC_DISCOVERY).getOrNull() ?: return null
        return parseOidcDiscoveryInfo(response)
    }
}

internal fun parseOidcUserInfo(response: String): OidcUserInfoUiModel? {
    val jsonBody = parseJsonBody(response) ?: return null
    return OidcUserInfoUiModel(
        email = extractJsonString(jsonBody, "email"),
        username = extractJsonString(jsonBody, "preferred_username"),
        subject = extractJsonString(jsonBody, "sub"),
    )
}

internal fun parseOidcDiscoveryInfo(response: String): OidcDiscoveryInfoUiModel? {
    val jsonBody = parseJsonBody(response) ?: return null
    return OidcDiscoveryInfoUiModel(
        issuer = extractJsonString(jsonBody, "issuer"),
        authorizationEndpoint = extractJsonString(jsonBody, "authorization_endpoint"),
        tokenEndpoint = extractJsonString(jsonBody, "token_endpoint"),
        userInfoEndpoint = extractJsonString(jsonBody, "userinfo_endpoint"),
        jwksUri = extractJsonString(jsonBody, "jwks_uri"),
        revocationEndpoint = extractJsonString(jsonBody, "revocation_endpoint"),
        introspectionEndpoint = extractJsonString(jsonBody, "introspection_endpoint"),
        endSessionEndpoint = extractJsonString(jsonBody, "end_session_endpoint"),
        supportedGrantTypes = extractJsonStringArray(jsonBody, "grant_types_supported"),
    )
}

private fun parseJsonBody(response: String): String? {
    val bodyStartIndex = response.indexOf('{')
    if (bodyStartIndex < 0) return null
    val jsonBody = response.substring(bodyStartIndex).trim()
    if (!jsonBody.startsWith("{")) return null
    return jsonBody
}

private fun extractJsonString(jsonBody: String, key: String): String? {
    val keyToken = "\"$key\""
    val keyIndex = jsonBody.indexOf(keyToken)
    if (keyIndex < 0) return null

    var index = keyIndex + keyToken.length
    while (index < jsonBody.length && jsonBody[index].isWhitespace()) {
        index++
    }
    if (jsonBody.getOrNull(index) != ':') return null

    index++
    while (index < jsonBody.length && jsonBody[index].isWhitespace()) {
        index++
    }

    val (value, _) = parseQuotedJsonValue(jsonBody, index) ?: return null
    return value.ifBlank { null }
}

private fun extractJsonStringArray(jsonBody: String, key: String): List<String> {
    val keyToken = "\"$key\""
    val keyIndex = jsonBody.indexOf(keyToken)
    if (keyIndex < 0) return emptyList()

    var index = keyIndex + keyToken.length
    while (index < jsonBody.length && jsonBody[index].isWhitespace()) {
        index++
    }
    if (jsonBody.getOrNull(index) != ':') return emptyList()

    index++
    while (index < jsonBody.length && jsonBody[index].isWhitespace()) {
        index++
    }
    if (jsonBody.getOrNull(index) != '[') return emptyList()

    val values = mutableListOf<String>()
    index++
    while (index < jsonBody.length) {
        while (index < jsonBody.length && (jsonBody[index].isWhitespace() || jsonBody[index] == ',')) {
            index++
        }
        when (jsonBody.getOrNull(index)) {
            ']' -> return values
            '"' -> {
                val (value, nextIndex) = parseQuotedJsonValue(jsonBody, index) ?: return values
                if (value.isNotBlank()) {
                    values += value
                }
                index = nextIndex
            }
            else -> index++
        }
    }
    return values
}

private fun parseQuotedJsonValue(source: String, startIndex: Int): Pair<String, Int>? {
    if (source.getOrNull(startIndex) != '"') return null

    val rawValue = StringBuilder()
    var index = startIndex + 1
    var escaped = false
    while (index < source.length) {
        val current = source[index]
        when {
            escaped -> {
                rawValue.append('\\')
                rawValue.append(current)
                escaped = false
            }
            current == '\\' -> escaped = true
            current == '"' -> return rawValue.toString().decodeJsonString() to (index + 1)
            else -> rawValue.append(current)
        }
        index++
    }
    return null
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

            val next = this@decodeJsonString.getOrNull(index + 1)
            when (next) {
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
