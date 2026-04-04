package info.meuse24.m24bikestats.presentation.apitest

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.meuse24.m24bikestats.BuildConfig
import info.meuse24.m24bikestats.api.BoschEndpoint
import info.meuse24.m24bikestats.api.FetchBoschDataUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

data class ApiTestUiState(
    val selectedEndpoint: BoschEndpoint = BoschEndpoint.entries.first(),
    val jsonOutput: String = "",
    val isLoading: Boolean = false,
)

class ApiTestViewModel(
    private val fetchBoschData: FetchBoschDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApiTestUiState())
    val uiState: StateFlow<ApiTestUiState> = _uiState.asStateFlow()

    fun selectEndpoint(endpoint: BoschEndpoint) {
        _uiState.update { it.copy(selectedEndpoint = endpoint, jsonOutput = "") }
    }

    fun fetch() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = fetchBoschData(_uiState.value.selectedEndpoint)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    jsonOutput = result.getOrElse { e -> "Fehler: ${e.message}" }
                )
            }
        }
    }

    fun runAllEndpoints() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, jsonOutput = "") }

            val activitiesResponse = fetchBoschData(BoschEndpoint.SMART_ACTIVITIES)
            val bikesResponse = fetchBoschData(BoschEndpoint.SMART_BIKES)
            val activityId = extractFirstActivityId(activitiesResponse.getOrNull())
            val bikeId = extractFirstBikeId(bikesResponse.getOrNull())
            val requests = buildRunAllRequests(
                activityId = activityId,
                bikeId = bikeId,
                activitiesResponse = activitiesResponse.getOrNull(),
            ).toMutableList()
            val seenUrls = requests.map { it.url }.toMutableSet()
            val seededResults = mapOf(
                BoschEndpoint.SMART_ACTIVITIES.name to activitiesResponse,
                BoschEndpoint.SMART_BIKES.name to bikesResponse,
            )

            val reportBuilder = StringBuilder(
                buildString {
                    appendLine("=== Bosch Endpoint Batch Test ===")
                    appendLine("Initial endpoints: ${requests.size}")
                    appendLine()
                }
            )

            reportBuilder.appendLine("Resolved IDs:")
            reportBuilder.appendLine("activityId: ${activityId ?: "n/a"}")
            reportBuilder.appendLine("bikeId: ${bikeId ?: "n/a"}")
            reportBuilder.appendLine()

            var index = 0
            while (index < requests.size) {
                val request = requests[index++]
                debugLog("START ${request.debugName} -> ${request.url}")

                val result = seededResults[request.debugName] ?: fetchBoschData(request)
                val body = result.getOrElse { error -> "Fehler: ${error.message}" }

                reportBuilder.appendLine("=== ${request.label} (${request.debugName}) ===")
                reportBuilder.appendLine("URL: ${request.url}")
                reportBuilder.appendLine(body)
                reportBuilder.appendLine()

                debugLog(
                    buildString {
                        appendLine("RESULT ${request.debugName}")
                        appendLine("URL: ${request.url}")
                        appendLine(body)
                    }
                )

                _uiState.update {
                    it.copy(jsonOutput = reportBuilder.toString())
                }

                if (request.url.contains("/activity/smart-system/v1/activities?")) {
                    buildAdditionalActivityRequests(result.getOrNull())
                        .filter { seenUrls.add(it.url) }
                        .forEach(requests::add)
                }
            }

            reportBuilder.appendLine("Executed endpoints: $index")
            reportBuilder.appendLine()
            debugLog("BATCH_TEST_COMPLETED")
            _uiState.update {
                it.copy(
                    isLoading = false,
                    jsonOutput = reportBuilder.toString()
                )
            }
        }
    }

    fun clear() {
        _uiState.update { it.copy(jsonOutput = "") }
    }

    companion object {
        const val LOG_TAG = "BoschEndpointBatch"
    }

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) {
            Log.i(LOG_TAG, message)
        }
    }

    private fun extractFirstActivityId(response: String?): String? {
        val json = extractJsonBody(response) ?: return null
        val activities = JSONObject(json).optJSONArray("activitySummaries") ?: return null
        return activities.optJSONObject(0)?.optString("id")?.takeIf { it.isNotBlank() }
    }

    private fun extractFirstBikeId(response: String?): String? {
        val json = extractJsonBody(response) ?: return null
        val bikes = JSONObject(json).optJSONArray("bikes") ?: return null
        return bikes.optJSONObject(0)?.optString("id")?.takeIf { it.isNotBlank() }
    }
}
