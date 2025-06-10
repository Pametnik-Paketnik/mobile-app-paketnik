package com.jvn.myapplication.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jvn.myapplication.data.repository.AuthRepository
import com.jvn.myapplication.data.model.LoginResponse
import com.jvn.myapplication.data.model.TwoFactorMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            authRepository.login(email, password)
                .onSuccess { loginResponse ->
                    if (loginResponse.twoFactorRequired == true) {
                        // 2FA is required
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            twoFactorRequired = true,
                            tempToken = loginResponse.tempToken,
                            available2FAMethods = loginResponse.available_2fa_methods ?: emptyList()
                        )
                    } else {
                        // Standard login successful
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isAuthenticated = true
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

    fun register(name: String, surname: String, email: String, password: String, confirmPassword: String, userType: String) {
        if (password != confirmPassword) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Passwords do not match",
                isLoading = false
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            try {
                val response = authRepository.register(name, surname, email, password, userType)
                if (response.success) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        successMessage = response.message,
                        errorMessage = null
                    )
                    // Note: Token and user info are already saved in the repository
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = response.message
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Registration failed"
                )
            }
        }
    }

    fun verifyTotp(code: String) {
        val tempToken = _uiState.value.tempToken
        if (tempToken == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "No temp token available")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            authRepository.verifyTotpLogin(tempToken, code)
                .onSuccess { loginResponse ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        twoFactorRequired = false
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "TOTP verification failed"
                    )
                }
        }
    }

    fun verifyFace(faceImageFile: File) {
        val tempToken = _uiState.value.tempToken
        if (tempToken == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "No temp token available")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            authRepository.verifyFaceLogin(tempToken, faceImageFile)
                .onSuccess { loginResponse ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        twoFactorRequired = false
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Face verification failed"
                    )
                }
        }
    }

    fun selectTwoFactorMethod(methodType: String) {
        _uiState.value = _uiState.value.copy(selectedTwoFactorMethod = methodType)
    }

    fun resetTwoFactorFlow() {
        _uiState.value = _uiState.value.copy(
            twoFactorRequired = false,
            tempToken = null,
            available2FAMethods = emptyList(),
            selectedTwoFactorMethod = null
        )
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
    val successMessage: String? = null,
    val twoFactorRequired: Boolean = false,
    val tempToken: String? = null,
    val available2FAMethods: List<TwoFactorMethod> = emptyList(),
    val selectedTwoFactorMethod: String? = null
)