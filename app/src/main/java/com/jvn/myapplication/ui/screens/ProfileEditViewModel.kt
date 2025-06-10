package com.jvn.myapplication.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jvn.myapplication.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileEditViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileEditUiState())
    val uiState: StateFlow<ProfileEditUiState> = _uiState.asStateFlow()

    fun updateProfile(
        userId: Int,
        name: String,
        surname: String,
        email: String,
        confirmPassword: String,
        userType: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null,
                passwordError = null
            )

            // First verify the password
            authRepository.verifyPassword(email, confirmPassword)
                .onSuccess { isValidPassword ->
                    if (isValidPassword) {
                        // Password is correct, proceed with update
                        updateUserProfile(userId, name, surname, email, confirmPassword, userType)
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
        }
    }

    private suspend fun updateUserProfile(
        userId: Int,
        name: String,
        surname: String,
        email: String,
        password: String,
        userType: String
    ) {
        authRepository.updateUserProfile(userId, name, surname, email, password, userType)
            .onSuccess { updatedUser ->
                println("🔍 DEBUG - ProfileEditViewModel: Profile update successful for user: ${updatedUser.name} ${updatedUser.surname}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isUpdateSuccessful = true,
                    successMessage = "Profile updated successfully!"
                )
            }
            .onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = exception.message ?: "Failed to update profile"
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

data class ProfileEditUiState(
    val isLoading: Boolean = false,
    val isUpdateSuccessful: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val passwordError: String? = null
) 