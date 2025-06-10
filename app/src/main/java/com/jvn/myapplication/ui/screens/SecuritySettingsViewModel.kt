package com.jvn.myapplication.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jvn.myapplication.data.repository.AuthRepository
import com.jvn.myapplication.data.repository.FaceAuthRepository
import com.jvn.myapplication.data.repository.TotpRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SecuritySettingsViewModel(
    private val authRepository: AuthRepository,
    private val faceAuthRepository: FaceAuthRepository,
    private val totpRepository: TotpRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecuritySettingsUiState())
    val uiState: StateFlow<SecuritySettingsUiState> = _uiState.asStateFlow()

    val isFace2FAEnabled = authRepository.isFace2FAEnabled()
    
    private val _isTotpEnabled = MutableStateFlow(false)
    val isTotpEnabled: StateFlow<Boolean> = _isTotpEnabled.asStateFlow()

    init {
        loadTotpStatus()
    }

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

    fun setupTotp() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSettingUpTotp = true)
            
            totpRepository.setupTotp()
                .onSuccess { totpResponse ->
                    // Don't enable TOTP yet - wait for verification
                    _uiState.value = _uiState.value.copy(
                        isSettingUpTotp = false,
                        totpSecret = totpResponse.secret,
                        showTotpSetup = true
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isSettingUpTotp = false,
                        errorMessage = exception.message ?: "Failed to setup TOTP"
                    )
                }
        }
    }

    fun disableTotp() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDisablingTotp = true)
            
            totpRepository.disableTotp()
                .onSuccess { response ->
                    _isTotpEnabled.value = false
                    _uiState.value = _uiState.value.copy(
                        isDisablingTotp = false,
                        totpSecret = null,
                        showTotpSetup = false,
                        successMessage = response.message
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isDisablingTotp = false,
                        errorMessage = exception.message ?: "Failed to disable TOTP"
                    )
                }
        }
    }

    fun proceedToTotpVerification() {
        _uiState.value = _uiState.value.copy(
            showTotpSetup = false,
            showTotpVerification = true
        )
    }

    fun verifyTotpSetup(code: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isVerifyingTotp = true)
            
            totpRepository.verifyTotpSetup(code)
                .onSuccess { verifyResponse ->
                    if (verifyResponse.success) {
                        // Verification successful - enable TOTP
                        _isTotpEnabled.value = true
                        _uiState.value = _uiState.value.copy(
                            isVerifyingTotp = false,
                            showTotpVerification = false,
                            totpSecret = null,
                            successMessage = verifyResponse.message
                        )
                    } else {
                        // Verification failed - keep TOTP disabled
                        _uiState.value = _uiState.value.copy(
                            isVerifyingTotp = false,
                            errorMessage = verifyResponse.message
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isVerifyingTotp = false,
                        errorMessage = exception.message ?: "Failed to verify TOTP code"
                    )
                }
        }
    }

    fun dismissTotpSetup() {
        _uiState.value = _uiState.value.copy(
            showTotpSetup = false,
            totpSecret = null
        )
    }

    fun dismissTotpVerification() {
        _uiState.value = _uiState.value.copy(
            showTotpVerification = false,
            totpSecret = null
        )
    }

    private fun loadTotpStatus() {
        viewModelScope.launch {
            totpRepository.getTotpStatus()
                .onSuccess { statusResponse ->
                    _isTotpEnabled.value = statusResponse.enabled
                    println("🔍 DEBUG - SecuritySettingsViewModel: TOTP status loaded: ${statusResponse.enabled}")
                }
                .onFailure { exception ->
                    println("🔍 DEBUG - SecuritySettingsViewModel: Failed to load TOTP status: ${exception.message}")
                    // If we can't load status, assume it's disabled
                    _isTotpEnabled.value = false
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

data class SecuritySettingsUiState(
    val isDeleting: Boolean = false,
    val isCheckingStatus: Boolean = false,
    val requiresFaceVerification: Boolean = false,
    val isSettingUpTotp: Boolean = false,
    val isDisablingTotp: Boolean = false,
    val isVerifyingTotp: Boolean = false,
    val showTotpSetup: Boolean = false,
    val showTotpVerification: Boolean = false,
    val totpSecret: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
) 