package com.pressione.iperteso.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pressione.iperteso.data.repository.AuthError
import com.pressione.iperteso.data.repository.AuthRepository
import com.pressione.iperteso.domain.model.AuthSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val session: AuthSession? = null,
    val error: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = authRepository.login(username, password)

            result.fold(
                onSuccess = { session ->
                    _uiState.value = AuthUiState(
                        isLoggedIn = true,
                        session = session
                    )
                },
                onFailure = { error ->
                    val message = when (error) {
                        is AuthError.InvalidCredentials -> "Username o password non validi"
                        is AuthError.NetworkError -> "Errore di connessione: ${error.message}"
                        else -> "Si è verificato un errore"
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = message
                    )
                }
            )
        }
    }

    fun logout() {
        _uiState.value = AuthUiState()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
