package com.jvn.myapplication.ui.face

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jvn.myapplication.data.repository.FaceAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FaceAuthViewModel(
    private val faceAuthRepository: FaceAuthRepository,
    private val userId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(FaceAuthUiState())
    val uiState: StateFlow<FaceAuthUiState> = _uiState.asStateFlow()

    fun registerWithVideo(videoUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                errorMessage = null,
                currentStep = "Extracting frames from 10-second video..."
            )

            faceAuthRepository.registerFaceWithVideo(videoUri, userId) { progress ->
                _uiState.value = _uiState.value.copy(currentStep = progress)
            }.onSuccess { message ->
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        isRegistrationComplete = true,
                        successMessage = message,
                        currentStep = "Registration complete!"
                    )
                    // Start monitoring training status
                    checkTrainingStatus()
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        errorMessage = exception.message ?: "Face registration failed",
                        currentStep = "Registration failed"
                    )
                }
        }
    }

    fun checkTrainingStatus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCheckingStatus = true,
                currentStep = "Checking training status..."
            )

            faceAuthRepository.getTrainingStatus()
                .onSuccess { statusResponse ->
                    val isTrainingComplete = statusResponse.status == "training_completed"
                    _uiState.value = _uiState.value.copy(
                        isCheckingStatus = false,
                        isTrainingComplete = isTrainingComplete,
                        trainingStatus = statusResponse.status,
                        statusMessage = statusResponse.message,
                        currentStep = if (isTrainingComplete) "Training complete! Face verification is ready." 
                                     else "Training in progress..."
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isCheckingStatus = false,
                        errorMessage = exception.message ?: "Status check failed",
                        currentStep = "Status check failed"
                    )
                }
        }
    }

    fun verifyFace(imageUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isVerifying = true,
                errorMessage = null,
                currentStep = "Verifying your identity..."
            )

            faceAuthRepository.verifyFace(imageUri)
                .onSuccess { verifyResponse ->
                    // Check if probability > 0.6 for successful verification
                    val isSuccessful = verifyResponse.probability > 0.5f
                    _uiState.value = _uiState.value.copy(
                        isVerifying = false,
                        isVerificationComplete = true,
                        isAuthenticated = isSuccessful,
                        verificationProbability = verifyResponse.probability,
                        successMessage = if (isSuccessful) 
                            "Identity verified! (${(verifyResponse.probability * 100).toInt()}% confidence)"
                        else 
                            "Identity not verified (${(verifyResponse.probability * 100).toInt()}% confidence)",
                        currentStep = if (isSuccessful) "Verification successful!" else "Verification failed"
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isVerifying = false,
                        errorMessage = exception.message ?: "Face verification failed",
                        currentStep = "Verification failed"
                    )
                }
        }
    }

    fun deleteFaceData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                errorMessage = null,
                currentStep = "Deleting face data..."
            )

            faceAuthRepository.deleteFaceData()
                .onSuccess { message ->
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        successMessage = message,
                        isDataDeleted = true,
                        currentStep = "Face data deleted successfully"
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        errorMessage = exception.message ?: "Failed to delete face data",
                        currentStep = "Deletion failed"
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

    fun setErrorMessage(message: String) {
        _uiState.value = _uiState.value.copy(
            errorMessage = message
        )
    }

    fun resetState() {
        _uiState.value = FaceAuthUiState()
    }
}

data class FaceAuthUiState(
    val isProcessing: Boolean = false,
    val isCheckingStatus: Boolean = false,
    val isVerifying: Boolean = false,
    val isRegistrationComplete: Boolean = false,
    val isTrainingComplete: Boolean = false,
    val isVerificationComplete: Boolean = false,
    val isAuthenticated: Boolean = false,
    val isDataDeleted: Boolean = false,
    val verificationProbability: Float = 0.0f,
    val trainingStatus: String = "",
    val currentStep: String = "",
    val statusMessage: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null
) 