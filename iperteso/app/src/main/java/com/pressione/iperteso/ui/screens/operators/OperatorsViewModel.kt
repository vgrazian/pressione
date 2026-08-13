package com.pressione.iperteso.ui.screens.operators

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pressione.iperteso.data.repository.AuthRepository
import com.pressione.iperteso.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OperatorsUiState(
    val isLoading: Boolean = true,
    val users: List<User> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val resetUser: User? = null,
    val editingEmailUser: User? = null
)

class OperatorsViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OperatorsUiState())
    val uiState: StateFlow<OperatorsUiState> = _uiState.asStateFlow()

    private var currentAdminUsername: String = ""

    fun initialize(adminUsername: String) {
        currentAdminUsername = adminUsername
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val users = authRepository.getAllUsers()
                _uiState.value = _uiState.value.copy(isLoading = false, users = users)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Errore di caricamento"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    fun toggleRole(user: User) {
        if (user.username == currentAdminUsername) {
            _uiState.value = _uiState.value.copy(errorMessage = "Non puoi cambiare il tuo ruolo")
            return
        }
        val newRole = if (user.role == "admin") "user" else "admin"
        viewModelScope.launch {
            try {
                authRepository.setUserRole(user.username, newRole)
                loadUsers()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Errore")
            }
        }
    }

    fun toggleActive(user: User, onConfirm: () -> Unit = {}) {
        if (user.username == currentAdminUsername) {
            _uiState.value = _uiState.value.copy(errorMessage = "Non puoi disattivare il tuo account")
            return
        }
        viewModelScope.launch {
            try {
                authRepository.setUserActive(user.username, !user.active)
                loadUsers()
                onConfirm()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Errore")
            }
        }
    }

    fun deleteUser(user: User) {
        if (user.username == currentAdminUsername) {
            _uiState.value = _uiState.value.copy(errorMessage = "Non puoi eliminare il tuo account")
            return
        }
        viewModelScope.launch {
            try {
                authRepository.hardDeleteUser(user.username)
                loadUsers()
                _uiState.value = _uiState.value.copy(successMessage = "Utente eliminato")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Errore")
            }
        }
    }

    fun createUser(username: String, email: String, password: String, role: String, active: Boolean) {
        viewModelScope.launch {
            try {
                val created = authRepository.createUser(username, email, password, role)
                created.getOrThrow()
                if (!active) {
                    authRepository.setUserActive(username.lowercase().trim(), false)
                }
                _uiState.value = _uiState.value.copy(successMessage = "Utente \"${username.lowercase().trim()}\" creato")
                loadUsers()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Errore di creazione")
            }
        }
    }

    fun updateEmail(user: User, newEmail: String) {
        viewModelScope.launch {
            try {
                authRepository.changeEmail(user.username, newEmail)
                loadUsers()
                _uiState.value = _uiState.value.copy(successMessage = "Email aggiornata")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Errore")
            }
        }
    }

    fun resetPassword(user: User, newPassword: String) {
        viewModelScope.launch {
            try {
                authRepository.adminResetPassword(currentAdminUsername, user.username, newPassword)
                _uiState.value = _uiState.value.copy(successMessage = "Password reimpostata per ${user.username}")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Errore")
            }
        }
    }

    fun openReset(user: User) {
        _uiState.value = _uiState.value.copy(resetUser = user)
    }

    fun closeReset() {
        _uiState.value = _uiState.value.copy(resetUser = null)
    }
}
