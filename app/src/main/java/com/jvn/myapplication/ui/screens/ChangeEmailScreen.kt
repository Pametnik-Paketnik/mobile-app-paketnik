package com.jvn.myapplication.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jvn.myapplication.data.repository.AuthRepository
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeEmailScreen(
    onBack: () -> Unit = {},
    onEmailChanged: () -> Unit = {},
    authRepository: AuthRepository
) {
    val context = LocalContext.current
    val changeEmailViewModel: ChangeEmailViewModel = viewModel {
        ChangeEmailViewModel(authRepository)
    }
    
    // State variables
    var isContentVisible by remember { mutableStateOf(false) }
    
    // User data
    val userId by authRepository.getUserId().collectAsState(initial = null)
    val currentEmail by authRepository.getEmail().collectAsState(initial = null)
    val currentName by authRepository.getName().collectAsState(initial = null)
    val currentSurname by authRepository.getSurname().collectAsState(initial = null)
    val currentUserType by authRepository.getUserType().collectAsState(initial = null)
    
    // Form state
    var newEmail by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    // UI state
    val uiState by changeEmailViewModel.uiState.collectAsState()

    // Initialize form
    LaunchedEffect(Unit) {
        delay(200)
        isContentVisible = true
    }

    // Handle successful update
    LaunchedEffect(uiState.isUpdateSuccessful) {
        if (uiState.isUpdateSuccessful) {
            // Show toast notification
            Toast.makeText(context, "Email changed successfully!", Toast.LENGTH_SHORT).show()
            delay(500)
            onEmailChanged()
            onBack()
        }
    }

    // Clear messages and reset success state when screen appears
    LaunchedEffect(Unit) {
        changeEmailViewModel.clearMessages()
        changeEmailViewModel.resetSuccessState()
    }

    // Airbnb-style color palette (matching ProfileEditScreen)
    val airbnbRed = Color(0xFFFF5A5F)
    val lightGray = Color(0xFFF7F7F7)
    val cardWhite = Color(0xFFFFFFFF)
    val textDark = Color(0xFF484848)
    val textLight = Color(0xFF767676)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightGray)
    ) {
        // Top Bar (matching ProfileEditScreen)
        TopAppBar(
            title = { Text("Change Email") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = airbnbRed,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )

        // Content (matching ProfileEditScreen)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Email Change Form
            AnimatedVisibility(
                visible = isContentVisible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(800, delayMillis = 100)
                ) + fadeIn(animationSpec = tween(800, delayMillis = 100))
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardWhite),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "Email Address",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = textDark,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Text(
                            text = "Current email: ${currentEmail ?: "Not available"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textLight,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        // New Email field
                        OutlinedTextField(
                            value = newEmail,
                            onValueChange = { 
                                newEmail = it
                                changeEmailViewModel.clearMessages()
                            },
                            label = { Text("New Email Address") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = airbnbRed,
                                focusedLabelColor = airbnbRed
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            enabled = !uiState.isLoading
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Confirm Current Password",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textDark,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = "To change your email, please confirm your current password",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textLight,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Password confirmation field
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { 
                                confirmPassword = it
                                changeEmailViewModel.clearMessages()
                            },
                            label = { Text("Current Password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = airbnbRed,
                                focusedLabelColor = airbnbRed
                            ),
                            enabled = !uiState.isLoading,
                            isError = uiState.passwordError != null
                        )

                        // Password error (matching ProfileEditScreen style)
                        uiState.passwordError?.let { error ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        // General error message
                        uiState.errorMessage?.let { error ->
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                                )
                            ) {
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Save button (matching ProfileEditScreen)
                        Button(
                            onClick = {
                                val userIdValue = userId
                                val currentNameValue = currentName
                                val currentSurnameValue = currentSurname
                                val currentUserTypeValue = currentUserType
                                
                                if (userIdValue != null && currentNameValue != null && 
                                    currentSurnameValue != null && currentUserTypeValue != null) {
                                    changeEmailViewModel.changeEmail(
                                        userId = userIdValue.toInt(),
                                        newEmail = newEmail,
                                        currentName = currentNameValue,
                                        currentSurname = currentSurnameValue,
                                        currentUserType = currentUserTypeValue,
                                        confirmPassword = confirmPassword
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = airbnbRed),
                            shape = RoundedCornerShape(16.dp),
                            enabled = !uiState.isLoading && 
                                    newEmail.isNotBlank() && 
                                    confirmPassword.isNotBlank() &&
                                    newEmail != currentEmail
                        ) {
                            if (uiState.isLoading) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Changing Email...")
                                }
                            } else {
                                Text(
                                    "Change Email",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Success message (matching ProfileEditScreen)
                        uiState.successMessage?.let { message ->
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                                )
                            ) {
                                Text(
                                    text = message,
                                    color = Color(0xFF4CAF50),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
} 