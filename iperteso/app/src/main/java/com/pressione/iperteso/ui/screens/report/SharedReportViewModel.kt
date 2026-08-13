package com.pressione.iperteso.ui.screens.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pressione.iperteso.data.remote.api.ReadingReportJson
import com.pressione.iperteso.data.remote.api.SharedReportApi
import com.pressione.iperteso.domain.model.Reading
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SharedReportError { NOT_FOUND, EXPIRED, PIN_WRONG, GENERIC }

data class SharedReportUiState(
    val isLoading: Boolean = true,
    val error: SharedReportError? = null,
    val isPinVerified: Boolean = false,
    val isExpired: Boolean = false,
    val username: String = "",
    val readings: List<Reading> = emptyList()
)

class SharedReportViewModel(
    private val api: SharedReportApi = SharedReportApi()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SharedReportUiState())
    val uiState: StateFlow<SharedReportUiState> = _uiState.asStateFlow()

    private var cachedReport: com.pressione.iperteso.data.remote.api.SharedReportResponse? = null

    fun loadReport(token: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val report = api.getSharedReportByToken(token)
                if (report == null) {
                    _uiState.update {
                        it.copy(isLoading = false, error = SharedReportError.NOT_FOUND)
                    }
                    return@launch
                }
                // Check expiry
                val expiresAt = runCatching { java.time.Instant.parse(report.expiresAt) }.getOrNull()
                if (expiresAt != null && expiresAt.isBefore(java.time.Instant.now())) {
                    _uiState.update {
                        it.copy(isLoading = false, isExpired = true, error = SharedReportError.EXPIRED)
                    }
                    return@launch
                }
                cachedReport = report
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = SharedReportError.GENERIC)
                }
            }
        }
    }

    fun verifyPin(pin: String) {
        val report = cachedReport ?: return
        viewModelScope.launch {
            val expected = report.pin
            if (expected != null && pin != expected) {
                _uiState.update { it.copy(error = SharedReportError.PIN_WRONG) }
                return@launch
            }
            // Decode report data
            val data = report.reportData
            val (username, readings) = if (data != null) {
                ReadingReportJson.jsonToReadings(data)
            } else {
                report.username to emptyList()
            }
            _uiState.update {
                it.copy(
                    isPinVerified = true,
                    error = null,
                    username = username.ifBlank { report.username },
                    readings = readings
                )
            }
        }
    }
}
