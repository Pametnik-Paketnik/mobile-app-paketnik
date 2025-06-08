package com.jvn.myapplication.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jvn.myapplication.data.repository.AuthRepository
import com.jvn.myapplication.data.repository.FaceAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SecuritySettingsViewModel(
    private val authRepository: AuthRepository,
    private val faceAuthRepository: FaceAuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecuritySettingsUiState())
    val uiState: StateFlow<SecuritySettingsUiState> = _uiState.asStateFlow()

    val isFace2FAEnabled = authRepository.isFace2FAEnabled()

    fun enableFace2FA() {
        viewModelScope.launch {
            authRepository.setFace2FAEnabled(true)
        }
    }

    fun disableFace2FA() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true)
            
            // Call delete API when disabling 2FA
            faceAuthRepository.deleteFaceData()
                .onSuccess {
                    authRepository.setFace2FAEnabled(false)
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        successMessage = "Face data deleted successfully"
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        errorMessage = exception.message ?: "Failed to delete face data"
                    )
                }
        }
    }

    fun checkFaceStatusOnLogin(): Boolean {
        var hasTrainedFaceData = false
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCheckingStatus = true)
            
            faceAuthRepository.getTrainingStatus()
                .onSuccess { statusResponse ->
                    val isTrainingComplete = statusResponse.status == "training_completed"
                    if (isTrainingComplete) {
                        // Enable 2FA if face data is trained
                        authRepository.setFace2FAEnabled(true)
                        hasTrainedFaceData = true
                    } else {
                        // Disable 2FA if no trained face data
                        authRepository.setFace2FAEnabled(false)
                    }
                    _uiState.value = _uiState.value.copy(
                        isCheckingStatus = false,
                        requiresFaceVerification = isTrainingComplete
                    )
                }
                .onFailure {
                    // If check fails, assume no face data and disable 2FA
                    authRepository.setFace2FAEnabled(false)
                    _uiState.value = _uiState.value.copy(
                        isCheckingStatus = false,
                        requiresFaceVerification = false
                    )
                }
        }
        return hasTrainedFaceData
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}

data class SecuritySettingsUiState(
    val isDeleting: Boolean = false,
    val isCheckingStatus: Boolean = false,
    val requiresFaceVerification: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) 