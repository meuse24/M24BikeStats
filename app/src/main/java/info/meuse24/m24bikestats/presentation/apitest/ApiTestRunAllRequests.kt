package info.meuse24.m24bikestats.presentation.apitest

import info.meuse24.m24bikestats.api.BoschEndpoint
import info.meuse24.m24bikestats.api.BoschRequest

internal fun buildRunAllRequests(
    activityId: String?,
    bikeId: String?,
): List<BoschRequest> = buildList {
    addAll(
        BoschEndpoint.entries
            .map { endpoint ->
                endpoint.toRequest(activityId = activityId, bikeId = bikeId)
            }
    )
}

internal fun buildAdditionalActivityRequests(activitiesResponse: String?): List<BoschRequest> {
    val json = extractJsonBody(activitiesResponse) ?: return emptyList()
    val seenUrls = mutableSetOf<String>()

    return buildList {
        fun addLinkedRequest(
            relation: String,
        ) {
            val url = extractLinkUrl(json, relation).orEmpty().trim()
            if (url.isBlank()) return
            if (!seenUrls.add(url)) return
            createLinkedActivityRequest(relation, url)?.let(::add)
        }

        addLinkedRequest(relation = "prev")
        addLinkedRequest(relation = "next")
        addLinkedRequest(relation = "last")
    }
}

private fun createLinkedActivityRequest(
    relation: String,
    url: String,
): BoschRequest? {
    val relationInfo = when (relation) {
        "prev" -> "vorherige Seite" to "SMART_ACTIVITIES_PREV_LINK"
        "next" -> "nächste Seite" to "SMART_ACTIVITIES_NEXT_LINK"
        "last" -> "letzte Seite" to "SMART_ACTIVITIES_LAST_LINK"
        else -> return null
    }
    val offset = extractOffset(url)
    val offsetLabel = offset?.let { ", offset=$it" }.orEmpty()
    val offsetDebug = offset?.let { "_OFFSET_$it" }.orEmpty()

    return BoschRequest(
        label = "Aktivitäten ${relationInfo.first} (links.$relation$offsetLabel)",
        baseUrl = "",
        path = "",
        debugName = relationInfo.second + offsetDebug,
        absoluteUrl = url,
    )
}

private fun extractOffset(url: String): Int? =
    Regex("""[?&]offset=(\d+)""")
        .find(url)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()

private fun extractLinkUrl(json: String, relation: String): String? =
    Regex(""""$relation"\s*:\s*"([^"]+)"""")
        .find(json)
        ?.groupValues
        ?.getOrNull(1)
