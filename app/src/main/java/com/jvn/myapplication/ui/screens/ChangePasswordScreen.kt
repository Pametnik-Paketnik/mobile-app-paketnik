package com.jvn.myapplication.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jvn.myapplication.data.repository.AuthRepository
import kotlinx.coroutines.delay
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    onBack: () -> Unit = {},
    onPasswordChanged: () -> Unit = {},
    authRepository: AuthRepository
) {
    // Airbnb-style color palette (matching other screens)
    val airbnbRed = Color(0xFFFF5A5F)
    val lightGray = Color(0xFFF7F7F7)
    val cardWhite = Color(0xFFFFFFFF)
    val textDark = Color(0xFF484848)
    val textLight = Color(0xFF767676)

    val context = LocalContext.current

    // ViewModels
    val changePasswordViewModel: ChangePasswordViewModel = viewModel {
        ChangePasswordViewModel(authRepository)
    }

    // State variables
    var isContentVisible by remember { mutableStateOf(false) }
    
    // User data
    val userId by authRepository.getUserId().collectAsState(initial = null)
    val currentName by authRepository.getName().collectAsState(initial = null)
    val currentSurname by authRepository.getSurname().collectAsState(initial = null)
    val currentEmail by authRepository.getEmail().collectAsState(initial = null)
    val currentUserType by authRepository.getUserType().collectAsState(initial = null)
    
    // Form state
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    
    // UI state
    val uiState by changePasswordViewModel.uiState.collectAsState()

    // Initialize form
    LaunchedEffect(Unit) {
        delay(200)
        isContentVisible = true
    }

    // Handle successful update
    LaunchedEffect(uiState.isUpdateSuccessful) {
        if (uiState.isUpdateSuccessful) {
            // Show toast notification
            Toast.makeText(context, "Password changed successfully!", Toast.LENGTH_SHORT).show()
            delay(500)
            onPasswordChanged()
            onBack()
        }
    }

    // Clear messages and reset success state when screen appears
    LaunchedEffect(Unit) {
        changePasswordViewModel.clearMessages()
        changePasswordViewModel.resetSuccessState()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightGray)
    ) {
        // Top Bar (matching other screens)
        TopAppBar(
            title = { Text("Change Password") },
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

        // Content (matching other screens)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Password Change Form
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
                            text = "Change Password",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = textDark,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Text(
                            text = "Create a new password for your account",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textLight,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // Current Password Section
                        Text(
                            text = "Current Password",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textDark,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = "Please enter your current password to continue",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textLight,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Current Password field
                        OutlinedTextField(
                            value = currentPassword,
                            onValueChange = { 
                                currentPassword = it
                                changePasswordViewModel.clearMessages()
                            },
                            label = { Text("Current Password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (showCurrentPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = airbnbRed,
                                focusedLabelColor = airbnbRed
                            ),
                            enabled = !uiState.isLoading,
                            isError = uiState.currentPasswordError != null,
                            trailingIcon = {
                                IconButton(onClick = { showCurrentPassword = !showCurrentPassword }) {
                                    Icon(
                                        imageVector = if (showCurrentPassword) Icons.Default.KeyboardArrowRight else Icons.Default.KeyboardArrowLeft,
                                        contentDescription = if (showCurrentPassword) "Hide password" else "Show password"
                                    )
                                }
                            }
                        )

                        // Current Password error
                        uiState.currentPasswordError?.let { error ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // New Password Section
                        Text(
                            text = "New Password",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textDark,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = "Password must be at least 6 characters with letters and numbers",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textLight,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // New Password field
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { 
                                newPassword = it
                                changePasswordViewModel.clearMessages()
                            },
                            label = { Text("New Password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = airbnbRed,
                                focusedLabelColor = airbnbRed
                            ),
                            enabled = !uiState.isLoading,
                            isError = uiState.newPasswordError != null,
                            trailingIcon = {
                                IconButton(onClick = { showNewPassword = !showNewPassword }) {
                                    Icon(
                                        imageVector = if (showNewPassword) Icons.Default.KeyboardArrowRight else Icons.Default.KeyboardArrowLeft,
                                        contentDescription = if (showNewPassword) "Hide password" else "Show password"
                                    )
                                }
                            }
                        )

                        // New Password error
                        uiState.newPasswordError?.let { error ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Confirm New Password field
                        OutlinedTextField(
                            value = confirmNewPassword,
                            onValueChange = { confirmNewPassword = it },
                            label = { Text("Confirm New Password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = airbnbRed,
                                focusedLabelColor = airbnbRed
                            ),
                            enabled = !uiState.isLoading,
                            isError = confirmNewPassword.isNotEmpty() && newPassword != confirmNewPassword,
                            trailingIcon = {
                                IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                    Icon(
                                        imageVector = if (showConfirmPassword) Icons.Default.KeyboardArrowRight else Icons.Default.KeyboardArrowLeft,
                                        contentDescription = if (showConfirmPassword) "Hide password" else "Show password"
                                    )
                                }
                            }
                        )

                        // Confirm Password validation
                        if (confirmNewPassword.isNotEmpty() && newPassword != confirmNewPassword) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Passwords do not match",
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

                        // Save button (matching other screens)
                        Button(
                            onClick = {
                                if (newPassword == confirmNewPassword && newPassword.isNotEmpty()) {
                                    val userIdValue = userId
                                    val currentNameValue = currentName
                                    val currentSurnameValue = currentSurname
                                    val currentEmailValue = currentEmail
                                    val currentUserTypeValue = currentUserType
                                    
                                    if (userIdValue != null && currentNameValue != null && 
                                        currentSurnameValue != null && currentEmailValue != null && 
                                        currentUserTypeValue != null) {
                                        changePasswordViewModel.changePassword(
                                            userId = userIdValue.toInt(),
                                            currentPassword = currentPassword,
                                            newPassword = newPassword,
                                            currentName = currentNameValue,
                                            currentSurname = currentSurnameValue,
                                            currentEmail = currentEmailValue,
                                            currentUserType = currentUserTypeValue
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = airbnbRed),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !uiState.isLoading && 
                                     currentPassword.isNotEmpty() && 
                                     newPassword.isNotEmpty() && 
                                     confirmNewPassword.isNotEmpty() &&
                                     newPassword == confirmNewPassword
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    "Change Password",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Success message
            uiState.successMessage?.let { message ->
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(600)
                    ) + fadeIn(animationSpec = tween(600))
                ) {
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