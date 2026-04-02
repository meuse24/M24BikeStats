package info.meuse24.m24bikestats.presentation.apitest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.meuse24.m24bikestats.domain.model.BoschEndpoint
import info.meuse24.m24bikestats.domain.usecase.FetchBoschDataUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    fun clear() {
        _uiState.update { it.copy(jsonOutput = "") }
    }
}
