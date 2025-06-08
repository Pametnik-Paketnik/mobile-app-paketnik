// File: ui/main/MainAuthViewModel.kt (Simplified - back to original)
package com.jvn.myapplication.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jvn.myapplication.data.repository.AuthRepository
import com.jvn.myapplication.data.repository.FaceAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainAuthViewModel(
    private val authRepository: AuthRepository,
    private val faceAuthRepository: FaceAuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState.CHECKING)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkAuthStatus()
        // Monitor token changes
        viewModelScope.launch {
            authRepository.getAuthToken().collect { token ->
                println("DEBUG: Token in ViewModel: $token")
                if (token.isNullOrEmpty()) {
                    _authState.value = AuthState.UNAUTHENTICATED
                } else {
                    // Only check face verification on initial auth or when state is not already authenticated
                    if (_authState.value != AuthState.AUTHENTICATED) {
                        checkFaceVerificationRequirement()
                    }
                }
                println("DEBUG: AuthState set to: ${_authState.value}")
            }
        }
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            val token = authRepository.getAuthToken().first()
            if (token.isNullOrEmpty()) {
                _authState.value = AuthState.UNAUTHENTICATED
            } else {
                checkFaceVerificationRequirement()
            }
        }
    }

    private suspend fun checkFaceVerificationRequirement() {
        // Always check training status first to determine if user has face data
        faceAuthRepository.getTrainingStatus()
            .onSuccess { statusResponse ->
                if (statusResponse.status == "training_completed") {
                    // User has trained face data - enable 2FA and require verification
                    authRepository.setFace2FAEnabled(true)
                    _authState.value = AuthState.REQUIRES_FACE_VERIFICATION
                } else {
                    // User has no trained face data - disable 2FA and proceed
                    authRepository.setFace2FAEnabled(false)
                    _authState.value = AuthState.AUTHENTICATED
                }
            }
            .onFailure {
                // If status check fails, assume no face data and disable 2FA
                authRepository.setFace2FAEnabled(false)
                _authState.value = AuthState.AUTHENTICATED
            }
    }

    fun completeFaceVerification() {
        _authState.value = AuthState.AUTHENTICATED
    }

    fun skipFaceVerificationAndDisable2FA() {
        viewModelScope.launch {
            authRepository.setFace2FAEnabled(false)
            _authState.value = AuthState.AUTHENTICATED
        }
    }

    fun logout() {
        viewModelScope.launch {
            println("DEBUG: ViewModel logout called")
            authRepository.logout()
            // The token change will automatically trigger state update
        }
    }
}

enum class AuthState {
    CHECKING,
    AUTHENTICATED,
    UNAUTHENTICATED,
    REQUIRES_FACE_VERIFICATION
}