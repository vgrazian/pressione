package com.pressione.iperteso.ui.screens.readings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pressione.iperteso.data.repository.ReadingRepository
import com.pressione.iperteso.domain.model.Category
import com.pressione.iperteso.domain.model.Reading
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReadingListUiState(
    val isLoading: Boolean = true,
    val readings: List<Reading> = emptyList(),
    val selectedCategory: Category? = null,
    val searchQuery: String = ""
)

class ReadingListViewModel(
    private val readingRepository: ReadingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReadingListUiState())
    val uiState: StateFlow<ReadingListUiState> = _uiState.asStateFlow()

    private var allReadings: List<Reading> = emptyList()
    private var currentUsername: String = ""

    fun initialize(username: String) {
        currentUsername = username
        viewModelScope.launch {
            readingRepository.getReadings(username).collect { readings ->
                allReadings = readings
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    readings = applyFilters(readings)
                )
            }
        }
    }

    fun setCategoryFilter(category: Category?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        _uiState.value = _uiState.value.copy(
            readings = applyFilters(allReadings)
        )
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        _uiState.value = _uiState.value.copy(
            readings = applyFilters(allReadings)
        )
    }

    fun deleteReading(id: String) {
        viewModelScope.launch {
            readingRepository.deleteReading(id)
        }
    }

    private fun applyFilters(readings: List<Reading>): List<Reading> {
        val state = _uiState.value
        var filtered = readings

        state.selectedCategory?.let { category ->
            filtered = when (category) {
                Category.OPTIMAL -> filtered.filter {
                    it.category in setOf(Category.OPTIMAL, Category.NORMAL, Category.HIGH_NORMAL)
                }
                Category.GRADE_1 -> filtered.filter {
                    it.category in setOf(Category.GRADE_1, Category.GRADE_2, Category.GRADE_3)
                }
                else -> filtered.filter { it.category == category }
            }
        }

        if (state.searchQuery.isNotBlank()) {
            val query = state.searchQuery.lowercase()
            filtered = filtered.filter { reading ->
                reading.notes.lowercase().contains(query) ||
                reading.systolic.toString().contains(query) ||
                reading.diastolic.toString().contains(query) ||
                reading.heartRate.toString().contains(query)
            }
        }

        return filtered
    }
}
