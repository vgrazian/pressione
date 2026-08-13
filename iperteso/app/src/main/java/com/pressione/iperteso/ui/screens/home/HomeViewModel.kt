package com.pressione.iperteso.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pressione.iperteso.data.repository.AuthRepository
import com.pressione.iperteso.data.repository.ReadingRepository
import com.pressione.iperteso.domain.model.AuthSession
import com.pressione.iperteso.domain.model.Reading
import com.pressione.iperteso.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val session: AuthSession? = null,
    val latestReading: Reading? = null,
    val recentReadings: List<Reading> = emptyList(),
    val readingCount: Int = 0,
    val avgSystolic: Float = 0f,
    val avgDiastolic: Float = 0f,
    val avgHeartRate: Float = 0f,
    val syncStatus: SyncStatus = SyncStatus.IDLE
)

enum class SyncStatus {
    IDLE, SYNCING, SUCCESS, ERROR
}

class HomeViewModel(
    private val readingRepository: ReadingRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var currentUsername: String = ""

    fun initialize(session: AuthSession) {
        currentUsername = session.username
        _uiState.value = _uiState.value.copy(
            session = session
        )

        viewModelScope.launch {
            // Load user profile
            val user = authRepository.getUserProfile(session.username)
            _uiState.value = _uiState.value.copy(user = user)

            // Refresh from server
            syncData()
        }

        // Observe readings
        viewModelScope.launch {
            readingRepository.getReadings(currentUsername).collect { readings ->
                if (readings.isNotEmpty()) {
                    val latest = readings.first()
                    val avgSys = readings.map { it.systolic }.average().toFloat()
                    val avgDia = readings.map { it.diastolic }.average().toFloat()
                    val avgHr = readings.map { it.heartRate }.average().toFloat()

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        latestReading = latest,
                        recentReadings = readings.take(5),
                        readingCount = readings.size,
                        avgSystolic = avgSys,
                        avgDiastolic = avgDia,
                        avgHeartRate = avgHr
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch { syncData() }
    }

    private suspend fun syncData() {
        _uiState.value = _uiState.value.copy(syncStatus = SyncStatus.SYNCING)
        try {
            readingRepository.refreshFromServer(currentUsername)
            readingRepository.syncPendingReadings(currentUsername)
            _uiState.value = _uiState.value.copy(syncStatus = SyncStatus.SUCCESS)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(syncStatus = SyncStatus.ERROR)
        }
    }
}
