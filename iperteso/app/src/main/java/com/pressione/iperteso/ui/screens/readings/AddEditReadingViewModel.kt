package com.pressione.iperteso.ui.screens.readings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.annotation.StringRes
import com.pressione.iperteso.R
import com.pressione.iperteso.data.repository.ReadingRepository
import com.pressione.iperteso.domain.model.Category
import com.pressione.iperteso.domain.model.Reading
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

data class AddEditUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val systolic: String = "",
    val diastolic: String = "",
    val heartRate: String = "",
    val notes: String = "",
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime = LocalTime.now(),
    val category: Category? = null,
    @StringRes val systolicError: Int? = null,
    @StringRes val diastolicError: Int? = null,
    @StringRes val heartRateError: Int? = null,
    @StringRes val duplicateError: Int? = null,
    val saved: Boolean = false,
    val isEditing: Boolean = false,
    val editingId: String? = null
)

class AddEditReadingViewModel(
    private val readingRepository: ReadingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    private var currentUsername: String = ""

    fun initializeForNew(username: String) {
        currentUsername = username
        _uiState.value = AddEditUiState()
    }

    fun initializeForEdit(username: String, reading: Reading) {
        currentUsername = username
        val instant = reading.timestamp
        val ldt = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
        _uiState.value = AddEditUiState(
            systolic = reading.systolic.toString(),
            diastolic = reading.diastolic.toString(),
            heartRate = reading.heartRate.toString(),
            notes = reading.notes,
            date = ldt.toLocalDate(),
            time = ldt.toLocalTime(),
            category = reading.category,
            isEditing = true,
            editingId = reading.id
        )
    }

    fun updateSystolic(value: String) {
        _uiState.value = _uiState.value.copy(
            systolic = value,
            systolicError = null,
            duplicateError = null
        )
        updateCategory()
    }

    fun updateDiastolic(value: String) {
        _uiState.value = _uiState.value.copy(
            diastolic = value,
            diastolicError = null,
            duplicateError = null
        )
        updateCategory()
    }

    fun updateHeartRate(value: String) {
        _uiState.value = _uiState.value.copy(
            heartRate = value,
            heartRateError = null
        )
    }

    fun updateNotes(value: String) {
        _uiState.value = _uiState.value.copy(notes = value)
    }

    fun updateDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(date = date, duplicateError = null)
    }

    fun updateTime(time: LocalTime) {
        _uiState.value = _uiState.value.copy(time = time, duplicateError = null)
    }

    private fun updateCategory() {
        val sys = _uiState.value.systolic.toIntOrNull()
        val dia = _uiState.value.diastolic.toIntOrNull()
        if (sys != null && dia != null) {
            _uiState.value = _uiState.value.copy(category = Category.classify(sys, dia))
        }
    }

    fun save() {
        val systolic = _uiState.value.systolic.toIntOrNull()
        val diastolic = _uiState.value.diastolic.toIntOrNull()
        val heartRate = _uiState.value.heartRate.toIntOrNull()

        // Validate
        var hasError = false

        if (systolic == null || systolic <= 0 || systolic >= 300) {
            _uiState.value = _uiState.value.copy(
                systolicError = R.string.add_edit_error_systolic
            )
            hasError = true
        }
        if (diastolic == null || diastolic <= 0 || diastolic >= 200) {
            _uiState.value = _uiState.value.copy(
                diastolicError = R.string.add_edit_error_diastolic
            )
            hasError = true
        }
        if (heartRate == null || heartRate <= 0 || heartRate >= 300) {
            _uiState.value = _uiState.value.copy(
                heartRateError = R.string.add_edit_error_heart_rate
            )
            hasError = true
        }
        if (systolic != null && diastolic != null && diastolic >= systolic) {
            _uiState.value = _uiState.value.copy(
                diastolicError = R.string.add_edit_error_dia_less
            )
            hasError = true
        }

        if (hasError) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, duplicateError = null)

            val timestamp = _uiState.value.date
                .atTime(_uiState.value.time)
                .atZone(ZoneId.systemDefault())
                .toInstant()

            // Check duplicates within 10 min (only for new readings)
            if (!_uiState.value.isEditing) {
                val duplicate = readingRepository.findDuplicate(
                    currentUsername,
                    timestamp.toEpochMilli()
                )
                if (duplicate != null) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        duplicateError = R.string.add_edit_error_duplicate
                    )
                    return@launch
                }
            }

            val reading = Reading(
                id = _uiState.value.editingId ?: UUID.randomUUID().toString(),
                username = currentUsername,
                systolic = systolic!!,
                diastolic = diastolic!!,
                heartRate = heartRate!!,
                timestamp = timestamp,
                notes = _uiState.value.notes,
                category = Category.classify(systolic, diastolic)
            )

            readingRepository.upsertReading(reading)

            _uiState.value = _uiState.value.copy(
                isSaving = false,
                saved = true
            )
        }
    }
}
