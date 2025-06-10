package com.jvn.myapplication.ui.screens

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jvn.myapplication.data.repository.BoxRepository
import com.jvn.myapplication.data.repository.CleanerRepository
import com.jvn.myapplication.data.model.ExtraOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class CleanerBoxOpenViewModel(
    private val boxRepository: BoxRepository,
    private val cleanerRepository: CleanerRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CleanerBoxOpenUiState())
    val uiState: StateFlow<CleanerBoxOpenUiState> = _uiState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var currentOrder: ExtraOrder? = null

    fun openBoxForOrder(boxId: Int, order: ExtraOrder) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                isBoxOpened = false,
                showConfirmationDialog = false
            )

            // Store the current order for later use
            currentOrder = order

            // Verify box ownership - check if the box belongs to the host of the order
            val hostId = order.reservation.host.id
            
            println("🔍 DEBUG - CleanerBoxOpenViewModel: Verifying box ownership - hostId: $hostId, boxId: $boxId")
            
            // Get host's boxes to verify ownership
            boxRepository.getBoxesByHost(hostId)
                .onSuccess { hostBoxes ->
                    val boxExists = hostBoxes.any { box -> 
                        box.boxId?.toIntOrNull() == boxId
                    }

                    if (!boxExists) {
                        println("🔍 DEBUG - CleanerBoxOpenViewModel: Box $boxId does not belong to host $hostId")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "This box does not belong to the guest. Please scan the correct box for order #${order.id}."
                        )
                        return@onSuccess
                    }

                    println("🔍 DEBUG - CleanerBoxOpenViewModel: Box ownership verified. Opening box $boxId")

                    // Now open the box using the host's credentials
                    boxRepository.openBox(boxId, hostId)
                        .onSuccess { response ->
                            println("🔍 DEBUG - CleanerBoxOpenViewModel: Box opened successfully")
                            
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
                                println("🔍 DEBUG - CleanerBoxOpenViewModel: Error playing audio: ${e.message}")
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = "Box opening signal sent, but audio error: ${e.message}"
                                )
                            }
                        }
                        .onFailure { exception ->
                            println("🔍 DEBUG - CleanerBoxOpenViewModel: Failed to open box: ${exception.message}")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = exception.message ?: "Failed to open box"
                            )
                        }
                }
                .onFailure { exception ->
                    println("🔍 DEBUG - CleanerBoxOpenViewModel: Failed to verify box ownership: ${exception.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to verify box ownership: ${exception.message}"
                    )
                }
        }
    }

    private fun convertBase64ToMp3(base64Data: String): File {
        try {
            println("🔍 DEBUG - CleanerBoxOpenViewModel: Converting base64 to MP3")
            
            // Decode base64 string to byte array
            val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
            
            // Create temporary MP3 file
            val tempFile = File.createTempFile("cleaner_box_audio", ".mp3", context.cacheDir)
            
            // Write bytes to file
            FileOutputStream(tempFile).use { fos ->
                fos.write(audioBytes)
            }
            
            println("🔍 DEBUG - CleanerBoxOpenViewModel: MP3 file created: ${tempFile.absolutePath}")
            println("🔍 DEBUG - CleanerBoxOpenViewModel: File size: ${tempFile.length()} bytes")
            
            return tempFile
        } catch (e: Exception) {
            println("🔍 DEBUG - CleanerBoxOpenViewModel: Error converting base64 to MP3: ${e.message}")
            throw e
        }
    }

    private fun playMp3FileLooping(mp3File: File) {
        try {
            println("🔍 DEBUG - CleanerBoxOpenViewModel: Starting MP3 looping playback")
            
            // Release any existing media player
            mediaPlayer?.release()
            
            // Create new media player with looping
            mediaPlayer = MediaPlayer().apply {
                setDataSource(mp3File.absolutePath)
                isLooping = true // Enable looping
                setOnPreparedListener { mp ->
                    println("🔍 DEBUG - CleanerBoxOpenViewModel: MediaPlayer prepared, starting looping playback")
                    mp.start()
                }
                setOnErrorListener { mp, what, extra ->
                    println("🔍 DEBUG - CleanerBoxOpenViewModel: MediaPlayer error: what=$what, extra=$extra")
                    mp.release()
                    mp3File.delete()
                    false
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            println("🔍 DEBUG - CleanerBoxOpenViewModel: Error playing MP3: ${e.message}")
            // Clean up the file if playback fails
            mp3File.delete()
            throw e
        }
    }

    fun confirmBoxOpening(wasSuccessful: Boolean) {
        println("🔍 DEBUG - CleanerBoxOpenViewModel: Box opening confirmed as ${if (wasSuccessful) "successful" else "failed"}")
        
        // Stop the looping audio
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.stop()
            }
            mp.release()
            mediaPlayer = null
        }
        
        if (wasSuccessful) {
            // Box opened successfully, now show the notes dialog
            _uiState.value = _uiState.value.copy(
                showConfirmationDialog = false,
                isBoxOpened = true,
                showNotesDialog = true,
                successMessage = null
            )
        } else {
            // Box opening failed
            _uiState.value = _uiState.value.copy(
                showConfirmationDialog = false,
                isBoxOpened = false,
                showNotesDialog = false,
                successMessage = "Box opening failed. Please try again. ❌"
            )
        }
    }

    fun submitFulfillment(orderId: Int, notes: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                showNotesDialog = false
            )

            try {
                cleanerRepository.fulfillOrder(orderId, notes)
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isBoxOpened = true,
                            successMessage = "✅ Order fulfilled successfully!"
                        )
                    }
                    .onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "❌ Failed to fulfill order: ${exception.message}"
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "❌ Error: ${e.message}"
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

    fun resetState() {
        // Stop any playing audio
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.stop()
            }
            mp.release()
            mediaPlayer = null
        }
        
        currentOrder = null
        
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isBoxOpened = false,
            showConfirmationDialog = false,
            showNotesDialog = false,
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

data class CleanerBoxOpenUiState(
    val isLoading: Boolean = false,
    val isBoxOpened: Boolean = false,
    val showConfirmationDialog: Boolean = false,
    val showNotesDialog: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) 