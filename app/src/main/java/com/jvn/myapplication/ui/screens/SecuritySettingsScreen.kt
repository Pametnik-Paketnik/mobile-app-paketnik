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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jvn.myapplication.data.repository.AuthRepository
import com.jvn.myapplication.data.repository.FaceAuthRepository
import com.jvn.myapplication.data.repository.TotpRepository
import com.jvn.myapplication.ui.face.FaceAuthScreen
import com.jvn.myapplication.ui.face.FaceAuthViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    onBack: () -> Unit = {}
) {
    // Airbnb-style color palette
    val airbnbRed = Color(0xFFFF5A5F)
    val darkGray = Color(0xFF484848)
    val lightGray = Color(0xFFF7F7F7)
    val cardWhite = Color(0xFFFFFFFF)
    val textDark = Color(0xFF484848)
    val textLight = Color(0xFF767676)
    val successGreen = Color(0xFF4CAF50)

    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val faceAuthRepository = remember { FaceAuthRepository(context) }
    val totpRepository = remember { TotpRepository(authRepository) }

    // ViewModels
    val securitySettingsViewModel: SecuritySettingsViewModel = viewModel {
        SecuritySettingsViewModel(authRepository, faceAuthRepository, totpRepository)
    }

    // State variables
    var isContentVisible by remember { mutableStateOf(false) }
    var showFaceVerification by remember { mutableStateOf(false) }
    
    // Load state from ViewModels
    val faceVerificationEnabled by securitySettingsViewModel.isFace2FAEnabled.collectAsState(initial = false)
    val totpEnabled by securitySettingsViewModel.isTotpEnabled.collectAsState()
    val securityUiState by securitySettingsViewModel.uiState.collectAsState()

    // User data
    val userId by authRepository.getUserId().collectAsState(initial = null)

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200)
        isContentVisible = true
    }

    // Handle face verification screen
    if (showFaceVerification && userId != null) {
        val faceAuthViewModel: FaceAuthViewModel = viewModel {
            FaceAuthViewModel(faceAuthRepository, userId!!)
        }
        FaceAuthScreen(
            faceAuthViewModel = faceAuthViewModel,
            onRegistrationSuccess = {
                showFaceVerification = false
                // Enable 2FA after successful setup
                securitySettingsViewModel.enableFace2FA()
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightGray)
    ) {
        // Top Bar
        TopAppBar(
            title = { Text("Security & Privacy") },
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

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Face Verification Section
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
                            text = "Biometric Security",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = textDark,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Face Verification Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Face,
                                        contentDescription = null,
                                        tint = airbnbRed,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Face Verification (2FA)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textDark
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Add an extra layer of security with face recognition",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textLight
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Switch(
                                checked = faceVerificationEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        // Show face verification when enabling
                                        showFaceVerification = true
                                    } else {
                                        // Delete face data when disabling
                                        securitySettingsViewModel.disableFace2FA()
                                    }
                                },
                                enabled = !securityUiState.isDeleting, // Disable while deleting
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = successGreen,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color.Gray.copy(alpha = 0.4f)
                                )
                            )
                        }

                        // TOTP Toggle
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = airbnbRed,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "TOTP (2FA)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textDark
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Use Google Authenticator or other TOTP apps for security",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textLight
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Switch(
                                checked = totpEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        securitySettingsViewModel.setupTotp()
                                    } else {
                                        securitySettingsViewModel.disableTotp()
                                    }
                                },
                                enabled = !securityUiState.isSettingUpTotp && !securityUiState.isDisablingTotp,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = successGreen,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color.Gray.copy(alpha = 0.4f)
                                )
                            )
                        }

                        // Status display
                        Spacer(modifier = Modifier.height(12.dp))
                        when {
                            securityUiState.isDeleting -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = airbnbRed
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Deleting face data...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = textLight,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            securityUiState.isSettingUpTotp -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = airbnbRed
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Setting up TOTP...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = textLight,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            securityUiState.isDisablingTotp -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = airbnbRed
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Disabling TOTP...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = textLight,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            faceVerificationEnabled || totpEnabled -> {
                                Column {
                                    if (faceVerificationEnabled) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = successGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Face Verification is active",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = successGreen,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    if (totpEnabled) {
                                        if (faceVerificationEnabled) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = successGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "TOTP is active",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = successGreen,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Error/Success messages
                        securityUiState.errorMessage?.let { error ->
                            LaunchedEffect(error) {
                                delay(3000)
                                securitySettingsViewModel.clearMessages()
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Red
                            )
                        }

                        securityUiState.successMessage?.let { success ->
                            LaunchedEffect(success) {
                                delay(3000)
                                securitySettingsViewModel.clearMessages()
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = success,
                                style = MaterialTheme.typography.bodySmall,
                                color = successGreen
                            )
                        }
                    }
                }
            }

            // Security Information
            AnimatedVisibility(
                visible = isContentVisible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(800, delayMillis = 200)
                ) + fadeIn(animationSpec = tween(800, delayMillis = 200))
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = airbnbRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Security Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = textDark
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "• Face verification adds an extra layer of security to your account\n" +
                                    "• Your biometric data is processed locally and never stored on our servers\n" +
                                    "• You can disable face verification at any time",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textLight
                        )
                    }
                }
            }
        }
    }

    // TOTP Setup Dialog
    if (securityUiState.showTotpSetup && securityUiState.totpSecret != null) {
        val clipboardManager = LocalClipboardManager.current
        
        AlertDialog(
            onDismissRequest = { securitySettingsViewModel.dismissTotpSetup() },
            title = {
                Text(
                    text = "TOTP Setup",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textDark
                )
            },
            text = {
                Column {
                    Text(
                        text = "Add this Key to your authenticator app:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textDark,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = lightGray),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = securityUiState.totpSecret!!,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = textDark,
                                modifier = Modifier.weight(1f)
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(securityUiState.totpSecret!!))
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Copy to clipboard",
                                    tint = airbnbRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "• Open Google Authenticator, Authy, or any other TOTP app\n" +
                                "• Add a new account manually\n" +
                                "• Enter the Key shown above\n" +
                                "• Your app will generate 6-digit codes every 30 seconds",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textLight
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { securitySettingsViewModel.proceedToTotpVerification() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = airbnbRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Done",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            containerColor = cardWhite,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // TOTP Verification Dialog
    if (securityUiState.showTotpVerification) {
        var verificationCode by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { securitySettingsViewModel.dismissTotpVerification() },
            title = {
                Text(
                    text = "Verify TOTP Setup",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textDark
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter the 6-digit code from your authenticator app to complete setup:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textDark,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = verificationCode,
                        onValueChange = { 
                            if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                verificationCode = it
                            }
                        },
                        label = { Text("6-digit code") },
                        placeholder = { Text("123456") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = airbnbRed,
                            focusedLabelColor = airbnbRed,
                            cursorColor = airbnbRed
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "• The code changes every 30 seconds\n• Make sure to enter the current code from your app",
                        style = MaterialTheme.typography.bodySmall,
                        color = textLight
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        if (verificationCode.length == 6) {
                            securitySettingsViewModel.verifyTotpSetup(verificationCode)
                        }
                    },
                    enabled = verificationCode.length == 6 && !securityUiState.isVerifyingTotp,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = airbnbRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (securityUiState.isVerifyingTotp) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Verify",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { securitySettingsViewModel.dismissTotpVerification() },
                    enabled = !securityUiState.isVerifyingTotp
                ) {
                    Text(
                        text = "Cancel",
                        color = textLight
                    )
                }
            },
            containerColor = cardWhite,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun SecuritySettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val textDark = Color(0xFF484848)
    val textLight = Color(0xFF767676)
    val airbnbRed = Color(0xFFFF5A5F)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = airbnbRed,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = textDark
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = textLight
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF4CAF50),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.Gray.copy(alpha = 0.4f)
            )
        )
    }
} 