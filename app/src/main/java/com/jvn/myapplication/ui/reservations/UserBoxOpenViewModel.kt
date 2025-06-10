package com.jvn.myapplication.ui.reservations

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jvn.myapplication.data.repository.BoxRepository
import com.jvn.myapplication.data.repository.ReservationRepository
import com.jvn.myapplication.data.model.Reservation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class UserBoxOpenViewModel(
    private val boxRepository: BoxRepository,
    private val reservationRepository: ReservationRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserBoxOpenUiState())
    val uiState: StateFlow<UserBoxOpenUiState> = _uiState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var currentReservation: Reservation? = null
    private var currentAction: BoxAction? = null

    sealed class BoxAction {
        object CheckIn : BoxAction()
        object CheckOut : BoxAction()
    }

    fun startCheckIn(reservation: Reservation, scannedBoxId: Int) {
        currentReservation = reservation
        currentAction = BoxAction.CheckIn
        verifyAndOpenBox(scannedBoxId, reservation)
    }

    fun startCheckOut(reservation: Reservation, scannedBoxId: Int) {
        currentReservation = reservation
        currentAction = BoxAction.CheckOut
        verifyAndOpenBox(scannedBoxId, reservation)
    }

    private fun verifyAndOpenBox(scannedBoxId: Int, reservation: Reservation) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                isBoxOpened = false,
                showConfirmationDialog = false
            )

            // Verify the scanned box matches the reservation's box
            val reservationBoxId = reservation.box?.boxId?.toIntOrNull()
            
            if (reservationBoxId == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Reservation does not have a valid box ID"
                )
                return@launch
            }

            if (scannedBoxId != reservationBoxId) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Wrong box! This is box #$scannedBoxId, but your reservation is for box #$reservationBoxId."
                )
                return@launch
            }

            println("🔍 DEBUG - UserBoxOpenViewModel: Box verification successful - scanned: $scannedBoxId, reservation: $reservationBoxId")

            // Get the host ID to verify ownership and open the box
            val hostId = reservation.host?.id
            if (hostId == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Cannot determine box owner for this reservation"
                )
                return@launch
            }

            // Open the box using the host's credentials
            boxRepository.openBox(scannedBoxId, hostId)
                .onSuccess { response ->
                    println("🔍 DEBUG - UserBoxOpenViewModel: Box opened successfully")
                    
                    // Convert base64 to MP3 and start looping audio
                    try {
                        val mp3File = convertBase64ToMp3(response.data)
                        playMp3FileLooping(mp3File)
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            showConfirmationDialog = true,
                            successMessage = null
                        )
                    } catch (e: Exception) {
                        println("🔍 DEBUG - UserBoxOpenViewModel: Error playing audio: ${e.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Box opening signal sent, but audio error: ${e.message}"
                        )
                    }
                }
                .onFailure { exception ->
                    println("🔍 DEBUG - UserBoxOpenViewModel: Failed to open box: ${exception.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Failed to open box"
                    )
                }
        }
    }

    private fun convertBase64ToMp3(base64Data: String): File {
        try {
            println("🔍 DEBUG - UserBoxOpenViewModel: Converting base64 to MP3")
            
            // Decode base64 string to byte array
            val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
            
            // Create temporary MP3 file
            val tempFile = File.createTempFile("user_box_audio", ".mp3", context.cacheDir)
            
            // Write bytes to file
            FileOutputStream(tempFile).use { fos ->
                fos.write(audioBytes)
            }
            
            println("🔍 DEBUG - UserBoxOpenViewModel: MP3 file created: ${tempFile.absolutePath}")
            println("🔍 DEBUG - UserBoxOpenViewModel: File size: ${tempFile.length()} bytes")
            
            return tempFile
        } catch (e: Exception) {
            println("🔍 DEBUG - UserBoxOpenViewModel: Error converting base64 to MP3: ${e.message}")
            throw e
        }
    }

    private fun playMp3FileLooping(mp3File: File) {
        try {
            println("🔍 DEBUG - UserBoxOpenViewModel: Starting MP3 looping playback")
            
            // Release any existing media player
            mediaPlayer?.release()
            
            // Create new media player with looping
            mediaPlayer = MediaPlayer().apply {
                setDataSource(mp3File.absolutePath)
                isLooping = true // Enable looping
                setOnPreparedListener { mp ->
                    println("🔍 DEBUG - UserBoxOpenViewModel: MediaPlayer prepared, starting looping playback")
                    mp.start()
                }
                setOnErrorListener { mp, what, extra ->
                    println("🔍 DEBUG - UserBoxOpenViewModel: MediaPlayer error: what=$what, extra=$extra")
                    mp.release()
                    mp3File.delete()
                    false
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            println("🔍 DEBUG - UserBoxOpenViewModel: Error playing MP3: ${e.message}")
            // Clean up the file if playback fails
            mp3File.delete()
            throw e
        }
    }

    fun confirmBoxOpening(wasSuccessful: Boolean) {
        println("🔍 DEBUG - UserBoxOpenViewModel: Box opening confirmed as ${if (wasSuccessful) "successful" else "failed"}")
        
        // Stop the looping audio
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.stop()
            }
            mp.release()
            mediaPlayer = null
        }
        
        if (wasSuccessful) {
            // Box opened successfully, now perform the actual check-in or check-out
            performReservationAction()
        } else {
            // Box opening failed
            _uiState.value = _uiState.value.copy(
                showConfirmationDialog = false,
                isBoxOpened = false,
                successMessage = "Box opening failed. Please try again. ❌"
            )
        }
    }

    private fun performReservationAction() {
        viewModelScope.launch {
            val reservation = currentReservation
            val action = currentAction
            
            if (reservation == null || action == null) {
                _uiState.value = _uiState.value.copy(
                    showConfirmationDialog = false,
                    errorMessage = "Invalid reservation or action"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                showConfirmationDialog = false,
                isLoading = true
            )

            when (action) {
                BoxAction.CheckIn -> {
                    reservationRepository.checkIn(reservation.id)
                        .onSuccess { checkInResponse ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isBoxOpened = true,
                                successMessage = "✅ Check-in successful! Welcome!"
                            )
                        }
                        .onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = "❌ Check-in failed: ${exception.message}"
                            )
                        }
                }
                BoxAction.CheckOut -> {
                    // First, update the checkoutAt timestamp to current time
                    val updateResult = reservationRepository.updateReservationTimestamp(
                        reservationId = reservation.id,
                        updateCheckout = true
                    )
                    
                    if (updateResult.isFailure) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "❌ Failed to update checkout time: ${updateResult.exceptionOrNull()?.message}"
                        )
                        return@launch
                    }
                    
                    // Then proceed with the actual checkout
                    reservationRepository.checkOut(reservation.id)
                        .onSuccess { checkOutResponse ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isBoxOpened = true,
                                successMessage = "✅ Check-out successful! Thank you for staying with us!"
                            )
                        }
                        .onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = "❌ Check-out failed: ${exception.message}"
                            )
                        }
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    fun resetState() {
        // Stop any playing audio
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.stop()
            }
            mp.release()
            mediaPlayer = null
        }
        
        currentReservation = null
        currentAction = null
        
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isBoxOpened = false,
            showConfirmationDialog = false,
            errorMessage = null,
            successMessage = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up media player when ViewModel is destroyed
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

data class UserBoxOpenUiState(
    val isLoading: Boolean = false,
    val isBoxOpened: Boolean = false,
    val showConfirmationDialog: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) 