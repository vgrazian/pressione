package com.pressione.iperteso.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pressione.iperteso.data.local.dao.SettingsDao
import com.pressione.iperteso.data.repository.MedicationRepository
import com.pressione.iperteso.domain.model.Medication
import com.pressione.iperteso.services.MedicationEventStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

data class MedicationUiState(
    val isLoading: Boolean = true,
    val medications: List<Medication> = emptyList(),
    val activeMedications: List<Medication> = emptyList(),
    val showAddDialog: Boolean = false,
    val editingMedication: Medication? = null
)

class MedicationViewModel(
    private val medicationRepository: MedicationRepository,
    private val settingsDao: SettingsDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(MedicationUiState())
    val uiState: StateFlow<MedicationUiState> = _uiState.asStateFlow()

    private var currentUsername: String = ""

    fun initialize(username: String) {
        currentUsername = username
        viewModelScope.launch {
            medicationRepository.getMedications(username).collect { medications ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    medications = medications,
                    activeMedications = medications.filter { it.isActive }
                )
            }
        }
        viewModelScope.launch {
            medicationRepository.refreshFromServer(username)
        }
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true, editingMedication = null)
    }

    fun showEditDialog(medication: Medication) {
        _uiState.value = _uiState.value.copy(showAddDialog = true, editingMedication = medication)
    }

    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false, editingMedication = null)
    }

    fun saveMedication(
        name: String, dosage: String, frequency: String, notes: String,
        startDate: Instant, endDate: Instant?
    ) {
        viewModelScope.launch {
            val isEdit = _uiState.value.editingMedication != null
            val medication = Medication(
                id = _uiState.value.editingMedication?.id ?: java.util.UUID.randomUUID().toString(),
                username = currentUsername,
                name = name, dosage = dosage, frequency = frequency, notes = notes,
                startDate = startDate, endDate = endDate
            )
            medicationRepository.upsertMedication(medication)
            MedicationEventStore.append(
                currentUsername,
                if (isEdit) "💊 Farmaco modificato: $name"
                else "💊 Farmaco aggiunto: $name",
                settingsDao
            )
            _uiState.value = _uiState.value.copy(showAddDialog = false, editingMedication = null)
        }
    }

    fun stopMedication(medication: Medication) {
        viewModelScope.launch {
            medicationRepository.upsertMedication(
                medication.copy(endDate = Instant.now())
            )
            MedicationEventStore.append(
                currentUsername,
                "💊 Farmaco interrotto: ${medication.name}",
                settingsDao
            )
        }
    }

    fun deleteMedication(id: String) {
        val name = _uiState.value.medications.find { it.id == id }?.name ?: ""
        viewModelScope.launch {
            medicationRepository.deleteMedication(id)
            if (name.isNotBlank()) {
                MedicationEventStore.append(
                    currentUsername,
                    "💊 Farmaco rimosso: $name",
                    settingsDao
                )
            }
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            medicationRepository.deleteAllForUser(currentUsername)
        }
    }
}
