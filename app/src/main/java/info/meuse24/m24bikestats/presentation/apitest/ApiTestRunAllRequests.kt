package info.meuse24.m24bikestats.presentation.apitest

import info.meuse24.m24bikestats.api.BoschEndpoint
import info.meuse24.m24bikestats.domain.model.BoschApiRequest
import java.net.URI

internal data class ApiTestRequest(
    val label: String,
    val debugName: String,
    val url: String,
    val request: BoschApiRequest?,
    val isLocalOnly: Boolean = false,
)

internal fun buildRunAllRequests(
    activityId: String?,
    bikeId: String?,
): List<ApiTestRequest> = BoschEndpoint.entries.map { endpoint ->
    if (endpoint == BoschEndpoint.TOKEN_INFO) {
        ApiTestRequest(
            label = endpoint.label,
            debugName = endpoint.name,
            url = "local://token-info",
            request = null,
            isLocalOnly = true,
        )
    } else {
        val request = endpoint.toRequest(activityId = activityId, bikeId = bikeId)
        ApiTestRequest(
            label = request.label,
            debugName = endpoint.name,
            url = request.url,
            request = request,
        )
    }
}

internal fun buildAdditionalActivityRequests(activitiesResponse: String?): List<ApiTestRequest> {
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
): ApiTestRequest? {
    val relationInfo = when (relation) {
        "prev" -> "vorherige Seite" to "SMART_ACTIVITIES_PREV_LINK"
        "next" -> "nächste Seite" to "SMART_ACTIVITIES_NEXT_LINK"
        "last" -> "letzte Seite" to "SMART_ACTIVITIES_LAST_LINK"
        else -> return null
    }
    val offset = extractOffset(url)
    val offsetLabel = offset?.let { ", offset=$it" }.orEmpty()
    val offsetDebug = offset?.let { "_OFFSET_$it" }.orEmpty()
    val request = toAbsoluteRequest(
        label = "Aktivitäten ${relationInfo.first} (links.$relation$offsetLabel)",
        url = url,
    ) ?: return null

    return ApiTestRequest(
        label = request.label,
        debugName = relationInfo.second + offsetDebug,
        url = request.url,
        request = request,
    )
}

private fun toAbsoluteRequest(
    label: String,
    url: String,
): BoschApiRequest? {
    val uri = runCatching { URI(url) }.getOrNull() ?: return null
    val scheme = uri.scheme ?: return null
    val authority = uri.rawAuthority ?: return null
    val baseUrl = "$scheme://$authority"
    val path = buildString {
        append(uri.rawPath.orEmpty())
        uri.rawQuery?.takeIf { it.isNotBlank() }?.let {
            append('?')
            append(it)
        }
    }
    return BoschApiRequest(
        label = label,
        baseUrl = baseUrl,
        path = path,
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
