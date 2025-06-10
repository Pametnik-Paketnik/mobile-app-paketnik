package com.jvn.myapplication.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jvn.myapplication.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChangeEmailViewModel(
    val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangeEmailUiState())
    val uiState: StateFlow<ChangeEmailUiState> = _uiState.asStateFlow()

    fun changeEmail(
        userId: Int,
        newEmail: String,
        currentName: String,
        currentSurname: String,
        currentUserType: String,
        confirmPassword: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null,
                passwordError = null
            )

            // First verify the password using current email (we need to get it)
            authRepository.getEmail().collect { currentEmail ->
                if (currentEmail != null) {
                    // Verify password
                    authRepository.verifyPassword(currentEmail, confirmPassword)
                        .onSuccess { isValidPassword ->
                            if (isValidPassword) {
                                // Password is correct, proceed with email update
                                updateUserEmail(userId, newEmail, currentName, currentSurname, confirmPassword, currentUserType)
                            } else {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    passwordError = "Incorrect password. Please try again."
                                )
                            }
                        }
                        .onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                passwordError = "Failed to verify password: ${exception.message}"
                            )
                        }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Unable to get current email address"
                    )
                }
                return@collect // Only process the first emission
            }
        }
    }

    private suspend fun updateUserEmail(
        userId: Int,
        newEmail: String,
        currentName: String,
        currentSurname: String,
        password: String,
        userType: String
    ) {
        authRepository.updateUserProfile(userId, currentName, currentSurname, newEmail, password, userType)
            .onSuccess { updatedUser ->
                println("🔍 DEBUG - ChangeEmailViewModel: Email update successful for user: ${updatedUser.email}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isUpdateSuccessful = true,
                    successMessage = "Email changed successfully!"
                )
            }
            .onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = exception.message ?: "Failed to change email"
                )
            }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null,
            passwordError = null
        )
    }

    fun resetSuccessState() {
        _uiState.value = _uiState.value.copy(
            isUpdateSuccessful = false
        )
    }
}

data class ChangeEmailUiState(
    val isLoading: Boolean = false,
    val isUpdateSuccessful: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val passwordError: String? = null
) 