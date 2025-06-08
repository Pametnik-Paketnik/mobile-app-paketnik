// File: ui/unlock/UnlockHistoryViewModel.kt
package com.jvn.myapplication.ui.unlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jvn.myapplication.data.model.UnlockHistoryWithUser
import com.jvn.myapplication.data.repository.BoxRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UnlockHistoryViewModel(
    private val boxRepository: BoxRepository,
    private val hostId: Int? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(UnlockHistoryUiState())
    val uiState: StateFlow<UnlockHistoryUiState> = _uiState.asStateFlow()

    init {
        loadUnlockHistory()
    }

    fun loadUnlockHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            if (hostId != null) {
                // Load unlock history for host's boxes only
                boxRepository.getUnlockHistoryByHost(hostId)
                    .onSuccess { history ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            unlockHistory = history,
                            errorMessage = null
                        )
                    }
                    .onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = exception.message ?: "Failed to load unlock history for your boxes"
                        )
                    }
            } else {
                // Load all unlock history (admin view)
                boxRepository.getAllUnlockHistory()
                    .onSuccess { history ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            unlockHistory = history,
                            errorMessage = null
                        )
                    }
                    .onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = exception.message ?: "Failed to load unlock history"
                        )
                    }
            }
        }
    }

    fun loadHistoryByBox(boxId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            boxRepository.getUnlockHistoryByBox(boxId)
                .onSuccess { history ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        unlockHistory = history,
                        errorMessage = null
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Failed to load unlock history"
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

data class UnlockHistoryUiState(
    val isLoading: Boolean = false,
    val unlockHistory: List<UnlockHistoryWithUser> = emptyList(),
    val errorMessage: String? = null
)