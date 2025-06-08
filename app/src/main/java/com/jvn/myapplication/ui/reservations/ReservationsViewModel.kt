package com.jvn.myapplication.ui.reservations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jvn.myapplication.data.model.Reservation
import com.jvn.myapplication.data.repository.ReservationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReservationsViewModel(
    private val reservationRepository: ReservationRepository,
    private val userIdString: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReservationsUiState())
    val uiState: StateFlow<ReservationsUiState> = _uiState.asStateFlow()

    fun loadReservations() {
        println("🔍 DEBUG - ReservationsViewModel.loadReservations(): Starting with userIdString: '$userIdString'")
        
        if (userIdString.isEmpty()) {
            println("🔍 DEBUG - ReservationsViewModel.loadReservations(): ERROR - userIdString is empty!")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "User ID not available"
            )
            return
        }

        println("🔍 DEBUG - ReservationsViewModel.loadReservations(): userIdString is not empty, proceeding...")
        
        val userId = userIdString.toIntOrNull()
        if (userId == null) {
            println("🔍 DEBUG - ReservationsViewModel.loadReservations(): ERROR - Cannot convert '$userIdString' to integer!")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Invalid user ID format"
            )
            return
        }

        println("🔍 DEBUG - ReservationsViewModel.loadReservations(): Converted userIdString '$userIdString' to integer: $userId")
        println("🔍 DEBUG - ReservationsViewModel.loadReservations(): Starting API call...")

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            println("🔍 DEBUG - ReservationsViewModel.loadReservations(): Set loading state, calling repository...")

            reservationRepository.getReservationsByGuest(userId)
                .onSuccess { reservations ->
                    println("🔍 DEBUG - ReservationsViewModel: Loaded ${reservations.size} reservations")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        reservations = reservations.sortedByDescending { it.checkinAt },
                        errorMessage = null
                    )
                }
                .onFailure { exception ->
                    println("🔍 DEBUG - ReservationsViewModel: Error loading reservations: ${exception.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Failed to load reservations"
                    )
                }
        }
    }

    fun checkIn(reservationId: Int) {
        viewModelScope.launch {
            // Add reservation to checking in set
            _uiState.value = _uiState.value.copy(
                checkingInReservations = _uiState.value.checkingInReservations + reservationId
            )

            reservationRepository.checkIn(reservationId)
                .onSuccess { checkInResponse ->
                    if (checkInResponse.success) {
                        println("🔍 DEBUG - ReservationsViewModel: Check-in successful: ${checkInResponse.message}")
                        // Reload reservations to get updated data
                        loadReservations()
                    } else {
                        println("🔍 DEBUG - ReservationsViewModel: Check-in failed: ${checkInResponse.message}")
                        _uiState.value = _uiState.value.copy(
                            errorMessage = checkInResponse.message
                        )
                    }
                }
                .onFailure { exception ->
                    println("🔍 DEBUG - ReservationsViewModel: Check-in exception: ${exception.message}")
                    _uiState.value = _uiState.value.copy(
                        errorMessage = exception.message ?: "Check-in failed"
                    )
                }
                .also {
                    // Remove reservation from checking in set
                    _uiState.value = _uiState.value.copy(
                        checkingInReservations = _uiState.value.checkingInReservations - reservationId
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

data class ReservationsUiState(
    val isLoading: Boolean = false,
    val reservations: List<Reservation> = emptyList(),
    val checkingInReservations: Set<Int> = emptySet(),
    val errorMessage: String? = null
) 