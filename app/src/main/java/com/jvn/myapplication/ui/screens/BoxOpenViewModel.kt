package com.jvn.myapplication.ui.screens

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jvn.myapplication.data.repository.BoxRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class BoxOpenViewModel(
    private val boxRepository: BoxRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(BoxOpenUiState())
    val uiState: StateFlow<BoxOpenUiState> = _uiState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null

    fun openBox(boxId: Int, hostId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                isBoxOpened = false,
                showConfirmationDialog = false
            )

            boxRepository.openBox(boxId, hostId)
                .onSuccess { response ->
                    println("🔍 DEBUG - BoxOpenViewModel: Box opened successfully")
                    
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
                        println("🔍 DEBUG - BoxOpenViewModel: Error playing audio: ${e.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Box opening signal sent, but audio error: ${e.message}"
                        )
                    }
                }
                .onFailure { exception ->
                    println("🔍 DEBUG - BoxOpenViewModel: Failed to open box: ${exception.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Failed to open box"
                    )
                }
        }
    }

    private fun convertBase64ToMp3(base64Data: String): File {
        try {
            println("🔍 DEBUG - BoxOpenViewModel: Converting base64 to MP3")
            
            // Decode base64 string to byte array
            val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
            
            // Create temporary MP3 file
            val tempFile = File.createTempFile("box_open_audio", ".mp3", context.cacheDir)
            
            // Write bytes to file
            FileOutputStream(tempFile).use { fos ->
                fos.write(audioBytes)
            }
            
            println("🔍 DEBUG - BoxOpenViewModel: MP3 file created: ${tempFile.absolutePath}")
            println("🔍 DEBUG - BoxOpenViewModel: File size: ${tempFile.length()} bytes")
            
            return tempFile
        } catch (e: Exception) {
            println("🔍 DEBUG - BoxOpenViewModel: Error converting base64 to MP3: ${e.message}")
            throw e
        }
    }

    private fun playMp3FileLooping(mp3File: File) {
        try {
            println("🔍 DEBUG - BoxOpenViewModel: Starting MP3 looping playback")
            
            // Release any existing media player
            mediaPlayer?.release()
            
            // Create new media player with looping
            mediaPlayer = MediaPlayer().apply {
                setDataSource(mp3File.absolutePath)
                isLooping = true // Enable looping
                setOnPreparedListener { mp ->
                    println("🔍 DEBUG - BoxOpenViewModel: MediaPlayer prepared, starting looping playback")
                    mp.start()
                }
                setOnErrorListener { mp, what, extra ->
                    println("🔍 DEBUG - BoxOpenViewModel: MediaPlayer error: what=$what, extra=$extra")
                    mp.release()
                    mp3File.delete()
                    false
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            println("🔍 DEBUG - BoxOpenViewModel: Error playing MP3: ${e.message}")
            // Clean up the file if playback fails
            mp3File.delete()
            throw e
        }
    }

    fun confirmBoxOpening(wasSuccessful: Boolean) {
        println("🔍 DEBUG - BoxOpenViewModel: Box opening confirmed as ${if (wasSuccessful) "successful" else "failed"}")
        
        // Stop the looping audio
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.stop()
            }
            mp.release()
            mediaPlayer = null
        }
        
        _uiState.value = _uiState.value.copy(
            showConfirmationDialog = false,
            isBoxOpened = wasSuccessful,
            successMessage = if (wasSuccessful) "Box opened successfully! ✅" else "Box opening failed. Please try again. ❌"
        )
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

data class BoxOpenUiState(
    val isLoading: Boolean = false,
    val isBoxOpened: Boolean = false,
    val showConfirmationDialog: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) 