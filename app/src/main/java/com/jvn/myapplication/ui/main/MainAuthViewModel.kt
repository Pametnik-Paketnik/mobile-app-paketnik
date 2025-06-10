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
        // Check training status to set face 2FA availability, but don't require verification here
        // The new login flow handles all 2FA verification including face
        faceAuthRepository.getTrainingStatus()
            .onSuccess { statusResponse ->
                if (statusResponse.status == "training_completed") {
                    // User has trained face data - enable 2FA but proceed to authenticated state
                    authRepository.setFace2FAEnabled(true)
                } else {
                    // User has no trained face data - disable 2FA
                    authRepository.setFace2FAEnabled(false)
                }
                _authState.value = AuthState.AUTHENTICATED
            }
            .onFailure {
                // If status check fails, assume no face data and disable 2FA
                authRepository.setFace2FAEnabled(false)
                _authState.value = AuthState.AUTHENTICATED
            }
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
    UNAUTHENTICATED
}