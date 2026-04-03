package info.meuse24.m24bikestats.data.remote

class BoschJsonBodyExtractor {
    fun extract(response: String): String? {
        return response.substringAfter("\n\n", missingDelimiterValue = response)
            .trim()
            .takeIf { it.startsWith("{") || it.startsWith("[") }
    }
}
