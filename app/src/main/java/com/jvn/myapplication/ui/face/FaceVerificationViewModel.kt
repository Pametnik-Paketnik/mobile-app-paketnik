// File: ui/face/FaceVerificationViewModel.kt
package com.jvn.myapplication.ui.face

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jvn.myapplication.data.repository.FaceVerificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FaceVerificationViewModel(
    private val faceVerificationRepository: FaceVerificationRepository,
    private val userId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(FaceVerificationUiState())
    val uiState: StateFlow<FaceVerificationUiState> = _uiState.asStateFlow()

    fun processVideo(videoUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                errorMessage = null
            )

            faceVerificationRepository.processVideoAndSubmit(videoUri, userId)
                .onSuccess { message ->
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        isVerificationComplete = true,
                        successMessage = message
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        errorMessage = exception.message ?: "Face verification failed"
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

data class FaceVerificationUiState(
    val isProcessing: Boolean = false,
    val isVerificationComplete: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)