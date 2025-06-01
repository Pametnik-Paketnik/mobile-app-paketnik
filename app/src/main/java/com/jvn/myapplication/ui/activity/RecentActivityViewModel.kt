// File: ui/activity/RecentActivityViewModel.kt (Debug Version)
package com.jvn.myapplication.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jvn.myapplication.data.model.UnlockHistoryWithUser
import com.jvn.myapplication.data.repository.BoxRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecentActivityViewModel(
    private val boxRepository: BoxRepository,
    private val userId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecentActivityUiState())
    val uiState: StateFlow<RecentActivityUiState> = _uiState.asStateFlow()

    init {
        loadUserHistory()
    }

    fun loadUserHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            // Debug: Print the userId being used
            println("🔍 DEBUG - RecentActivityViewModel: Using userId = '$userId'")

            boxRepository.getUnlockHistoryByUser(userId)
                .onSuccess { history ->
                    println("🔍 DEBUG - RecentActivityViewModel: API returned ${history.size} items")
                    // Sort by timestamp descending (most recent first)
                    val sortedHistory = history.sortedByDescending { it.timestamp }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        unlockHistory = sortedHistory,
                        errorMessage = null
                    )
                }
                .onFailure { exception ->
                    println("🔍 DEBUG - RecentActivityViewModel: API failed with: ${exception.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Failed to load your activity"
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

data class RecentActivityUiState(
    val isLoading: Boolean = false,
    val unlockHistory: List<UnlockHistoryWithUser> = emptyList(),
    val errorMessage: String? = null
)