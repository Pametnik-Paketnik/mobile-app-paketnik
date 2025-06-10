package com.jvn.myapplication.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jvn.myapplication.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChangePasswordViewModel(
    val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    fun changePassword(
        userId: Int,
        currentPassword: String,
        newPassword: String,
        currentName: String,
        currentSurname: String,
        currentEmail: String,
        currentUserType: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null,
                currentPasswordError = null,
                newPasswordError = null
            )

            // Validate new password
            val passwordValidation = validateNewPassword(newPassword)
            if (passwordValidation != null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    newPasswordError = passwordValidation
                )
                return@launch
            }

            // Verify current password
            authRepository.verifyPassword(currentEmail, currentPassword)
                .onSuccess { isValidPassword ->
                    if (isValidPassword) {
                        // Current password is correct, proceed with password update
                        updateUserPassword(userId, newPassword, currentName, currentSurname, currentEmail, currentUserType)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            currentPasswordError = "Incorrect current password. Please try again."
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentPasswordError = "Failed to verify current password: ${exception.message}"
                    )
                }
        }
    }

    private suspend fun updateUserPassword(
        userId: Int,
        newPassword: String,
        currentName: String,
        currentSurname: String,
        currentEmail: String,
        userType: String
    ) {
        authRepository.updateUserProfile(userId, currentName, currentSurname, currentEmail, newPassword, userType)
            .onSuccess { updatedUser ->
                println("🔍 DEBUG - ChangePasswordViewModel: Password update successful for user: ${updatedUser.email}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isUpdateSuccessful = true,
                    successMessage = "Password changed successfully!"
                )
            }
            .onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = exception.message ?: "Failed to change password"
                )
            }
    }

    private fun validateNewPassword(password: String): String? {
        return when {
            password.length < 6 -> "Password must be at least 6 characters long"
            password.isBlank() -> "Password cannot be empty"
            !password.any { it.isDigit() } -> "Password must contain at least one number"
            !password.any { it.isLetter() } -> "Password must contain at least one letter"
            else -> null
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null,
            currentPasswordError = null,
            newPasswordError = null
        )
    }

    fun resetSuccessState() {
        _uiState.value = _uiState.value.copy(
            isUpdateSuccessful = false
        )
    }
}

data class ChangePasswordUiState(
    val isLoading: Boolean = false,
    val isUpdateSuccessful: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val currentPasswordError: String? = null,
    val newPasswordError: String? = null
) 