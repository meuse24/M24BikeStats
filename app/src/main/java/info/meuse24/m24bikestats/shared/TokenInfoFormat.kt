package info.meuse24.m24bikestats.shared

object TokenInfoFormat {
    const val HEADER_MARKER = "=== HEADER ==="
    const val PAYLOAD_MARKER = "=== PAYLOAD ==="

    fun format(header: String, payload: String): String {
        return "$HEADER_MARKER\n$header\n\n$PAYLOAD_MARKER\n$payload"
    }

    fun extractPayload(text: String): String? {
        return text.substringAfter(PAYLOAD_MARKER, "")
            .trim()
            .takeIf { it.startsWith("{") }
    }
}
