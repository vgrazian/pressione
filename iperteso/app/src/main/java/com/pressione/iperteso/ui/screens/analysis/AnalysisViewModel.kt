package com.pressione.iperteso.ui.screens.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pressione.iperteso.data.repository.ReadingRepository
import com.pressione.iperteso.domain.model.Reading
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AnalysisUiState(
    val isLoading: Boolean = true,
    val readings: List<Reading> = emptyList(),
    val selectedTab: Int = 0,
    val periodDays: Int = 7
)

class AnalysisViewModel(
    private val readingRepository: ReadingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    fun initialize(username: String) {
        viewModelScope.launch {
            readingRepository.getReadings(username).collect { readings ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    readings = readings
                )
            }
        }
    }

    fun setTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun setPeriod(days: Int) {
        _uiState.value = _uiState.value.copy(periodDays = days)
    }

    fun getFilteredReadings(): List<Reading> {
        val cutoff = System.currentTimeMillis() - _uiState.value.periodDays * 24 * 60 * 60 * 1000L
        return _uiState.value.readings.filter {
            it.timestamp.toEpochMilli() >= cutoff
        }
    }
}
