// File: ui/main/MainAuthViewModel.kt
package com.jvn.myapplication.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jvn.myapplication.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainAuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState.CHECKING)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkAuthStatus()
        // Monitor token changes
        viewModelScope.launch {
            authRepository.getAuthToken().collect { token ->
                println("DEBUG: Token in ViewModel: $token")
                _authState.value = if (token.isNullOrEmpty()) {
                    AuthState.UNAUTHENTICATED
                } else {
                    AuthState.AUTHENTICATED
                }
                println("DEBUG: AuthState set to: ${_authState.value}")
            }
        }
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            val token = authRepository.getAuthToken().first()
            _authState.value = if (token.isNullOrEmpty()) {
                AuthState.UNAUTHENTICATED
            } else {
                AuthState.AUTHENTICATED
            }
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