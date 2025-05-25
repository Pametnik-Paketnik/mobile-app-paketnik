// File: ui/auth/AuthViewModel.kt
package com.jvn.myapplication.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jvn.myapplication.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            authRepository.login(username, password)
                .onSuccess { message ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Network error"
                    )
                }
        }
    }

    fun register(username: String, password: String, confirmPassword: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            authRepository.register(username, password, confirmPassword)
                .onSuccess { message ->
                    // Show success message first
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        registrationSuccess = true,
                        successMessage = "Registration successful! Redirecting..."
                    )

                    // Wait 2 seconds before auto-login
                    kotlinx.coroutines.delay(1000)

                    // After successful registration, automatically log the user in
                    authRepository.login(username, password)
                        .onSuccess {
                            _uiState.value = _uiState.value.copy(
                                isAuthenticated = true
                            )
                        }
                        .onFailure {
                            // Registration was successful but auto-login failed
                            _uiState.value = _uiState.value.copy(
                                successMessage = "Registration successful! Please login manually.",
                                registrationSuccess = false
                            )
                        }
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Network error"
                    )
                }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val registrationSuccess: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)