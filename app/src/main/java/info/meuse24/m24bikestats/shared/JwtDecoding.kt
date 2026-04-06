package info.meuse24.m24bikestats.shared

import java.util.Base64

internal data class DecodedJwtParts(
    val header: String,
    val payload: String,
)

internal fun decodeJwtBase64UrlSegment(segment: String): String? =
    runCatching {
        val paddedSegment = segment.padEnd((segment.length + 3) / 4 * 4, '=')
        String(Base64.getUrlDecoder().decode(paddedSegment))
    }.getOrNull()

internal fun decodeJwtParts(token: String): DecodedJwtParts? {
    val parts = token.split('.')
    if (parts.size < 2) return null

    val header = decodeJwtBase64UrlSegment(parts[0]) ?: return null
    val payload = decodeJwtBase64UrlSegment(parts[1]) ?: return null
    return DecodedJwtParts(header = header, payload = payload)
}
