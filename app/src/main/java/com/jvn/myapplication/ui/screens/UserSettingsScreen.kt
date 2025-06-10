package com.jvn.myapplication.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jvn.myapplication.data.repository.AuthRepository
import com.jvn.myapplication.data.repository.FaceAuthRepository
import com.jvn.myapplication.data.repository.TotpRepository
import com.jvn.myapplication.ui.face.FaceAuthScreen
import com.jvn.myapplication.ui.face.FaceAuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSettingsScreen(
    onLogout: () -> Unit = {}
) {
    // Initialize dependencies first
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    
    // State for navigation
    var showProfileEdit by remember { mutableStateOf(false) }
    var showChangeEmail by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    var profileUpdateTrigger by remember { mutableStateOf(0) }
    
    // Show ProfileEditScreen if needed
    if (showProfileEdit) {
        ProfileEditScreen(
            onBack = { showProfileEdit = false },
            onProfileUpdated = { 
                // Profile was updated successfully - trigger refresh
                println("🔍 DEBUG - UserSettingsScreen: Profile update completed, triggering refresh")
                profileUpdateTrigger++
                // Don't set showProfileEdit = false here, let the onBack handle navigation
            }
        )
        return
    }
    
    // Show ChangeEmailScreen if needed
    if (showChangeEmail) {
        ChangeEmailScreen(
            onBack = { showChangeEmail = false },
            onEmailChanged = {
                // Email was updated successfully - trigger refresh
                println("🔍 DEBUG - UserSettingsScreen: Email change completed, triggering refresh")
                profileUpdateTrigger++
                // Don't set showChangeEmail = false here, let the onBack handle navigation
            },
            authRepository = authRepository
        )
        return
    }
    
    // Show ChangePasswordScreen if needed
    if (showChangePassword) {
        ChangePasswordScreen(
            onBack = { showChangePassword = false },
            onPasswordChanged = {
                // Password was updated successfully - trigger refresh
                println("🔍 DEBUG - UserSettingsScreen: Password change completed, triggering refresh")
                profileUpdateTrigger++
                // Don't set showChangePassword = false here, let the onBack handle navigation
            },
            authRepository = authRepository
        )
        return
    }
    
    // Airbnb-style color palette
    val airbnbRed = Color(0xFFFF5A5F)
    val darkGray = Color(0xFF484848)
    val lightGray = Color(0xFFF7F7F7)
    val cardWhite = Color(0xFFFFFFFF)
    val textDark = Color(0xFF484848)
    val textLight = Color(0xFF767676)
    val faceAuthRepository = remember { FaceAuthRepository(context) }
    val totpRepository = remember { TotpRepository(authRepository) }

    // ViewModels
    val securitySettingsViewModel: SecuritySettingsViewModel = viewModel {
        SecuritySettingsViewModel(authRepository, faceAuthRepository, totpRepository)
    }

    // Animation state
    var isContentVisible by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showFaceVerification by remember { mutableStateOf(false) }

    // User data - will refresh when profileUpdateTrigger changes
    var userId by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf<String?>(null) }
    var surname by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf<String?>(null) }
    var userType by remember { mutableStateOf<String?>(null) }
    
    // Load user data and refresh when triggered
    LaunchedEffect(profileUpdateTrigger) {
        authRepository.getUserId().collect { userId = it }
    }
    
    LaunchedEffect(profileUpdateTrigger) {
        authRepository.getName().collect { 
            println("🔍 DEBUG - UserSettingsScreen: Name updated to: '$it'")
            name = it 
        }
    }
    
    LaunchedEffect(profileUpdateTrigger) {
        authRepository.getSurname().collect { 
            println("🔍 DEBUG - UserSettingsScreen: Surname updated to: '$it'")
            surname = it 
        }
    }
    
    LaunchedEffect(profileUpdateTrigger) {
        authRepository.getEmail().collect { email = it }
    }
    
    LaunchedEffect(profileUpdateTrigger) {
        authRepository.getUserType().collect { userType = it }
    }
    

    
    // Face verification state
    val faceVerificationEnabled by securitySettingsViewModel.isFace2FAEnabled.collectAsState(initial = false)
    val securityUiState by securitySettingsViewModel.uiState.collectAsState()

    LaunchedEffect(Unit, profileUpdateTrigger) {
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
            .verticalScroll(rememberScrollState())
    ) {
        // Clean header with solid color
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Solid background - no gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(airbnbRed)
            )

            // Profile section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                AnimatedVisibility(
                    visible = isContentVisible,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(animationSpec = tween(600))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Horizontal layout: avatar next to username
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Profile Avatar (smaller)
                            Box(
                                modifier = Modifier
                                    .size(48.dp) // Reduced from 80dp
                                    .background(
                                        Color.White.copy(alpha = 0.2f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (userType == "HOST") "🏢" else "👤",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column {
                                Text(
                                    text = if (name != null && surname != null) "$name $surname" else "User",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Start
                                )
                                Text(
                                    text = email ?: (if (userType == "HOST") "Host Account" else "Standard User"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f),
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                        
                        if (userId != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "ID: $userId",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Settings content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Account section
            AnimatedVisibility(
                visible = isContentVisible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(800, delayMillis = 100)
                ) + fadeIn(animationSpec = tween(800, delayMillis = 100))
            ) {
                SettingsSection(
                    title = "Account",
                    items = listOf(
                        SettingsItem(
                            icon = Icons.Default.Person,
                            title = "Profile Information",
                            subtitle = "Edit your personal details",
                            onClick = { showProfileEdit = true }
                        ),
                        SettingsItem(
                            icon = Icons.Default.Edit,
                            title = "Change Email",
                            subtitle = "Update your email address",
                            onClick = { showChangeEmail = true }
                        ),
                        SettingsItem(
                            icon = Icons.Default.Build,
                            title = "Change Password",
                            subtitle = "Update your login credentials",
                            onClick = { showChangePassword = true }
                        )
                    )
                )
            }

            // Security & Privacy section
            AnimatedVisibility(
                visible = isContentVisible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(800, delayMillis = 150)
                ) + fadeIn(animationSpec = tween(800, delayMillis = 150))
            ) {
                SecurityPrivacySection(
                    faceVerificationEnabled = faceVerificationEnabled,
                    totpEnabled = securitySettingsViewModel.isTotpEnabled.collectAsState().value,
                    isDeleting = securityUiState.isDeleting,
                    isSettingUpTotp = securityUiState.isSettingUpTotp,
                    isDisablingTotp = securityUiState.isDisablingTotp,
                    onToggleFaceVerification = { enabled ->
                        if (enabled) {
                            showFaceVerification = true
                        } else {
                            securitySettingsViewModel.disableFace2FA()
                        }
                    },
                    onToggleTotp = { enabled ->
                        if (enabled) {
                            securitySettingsViewModel.setupTotp()
                        } else {
                            securitySettingsViewModel.disableTotp()
                        }
                    }
                )
            }



            // Logout section
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
                    shape = RoundedCornerShape(20.dp),
                    onClick = { showLogoutDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    Color(0xFFFF5722).copy(alpha = 0.1f),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = null,
                                tint = Color(0xFFFF5722),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "Logout",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFF5722)
                            )
                            Text(
                                "Sign out of your account",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textLight
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp)) // Reduced spacing for proper layout
        }
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null,
                    tint = airbnbRed
                )
            },
            title = { 
                Text(
                    "Logout", 
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = { 
                Text("Are you sure you want to logout?") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        println("🔍 DEBUG - UserSettingsScreen: Logout button clicked")
                        onLogout()
                        showLogoutDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = airbnbRed)
                ) {
                    Text("Yes, logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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
private fun SettingsSection(
    title: String,
    items: List<SettingsItem>
) {
    val cardWhite = Color(0xFFFFFFFF)
    val textDark = Color(0xFF484848)

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
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textDark,
                modifier = Modifier.padding(8.dp)
            )
            
            items.forEachIndexed { index, item ->
                SettingsItemRow(item = item)
                if (index < items.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        color = Color.Gray.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsItemRow(item: SettingsItem) {
    val airbnbRed = Color(0xFFFF5A5F)
    val textDark = Color(0xFF484848)
    val textLight = Color(0xFF767676)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { item.onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    airbnbRed.copy(alpha = 0.1f),
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = airbnbRed,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = textDark
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = textLight
            )
        }
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            tint = textLight,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SecurityPrivacySection(
    faceVerificationEnabled: Boolean,
    totpEnabled: Boolean,
    isDeleting: Boolean,
    isSettingUpTotp: Boolean,
    isDisablingTotp: Boolean,
    onToggleFaceVerification: (Boolean) -> Unit,
    onToggleTotp: (Boolean) -> Unit
) {
    val airbnbRed = Color(0xFFFF5A5F)
    val cardWhite = Color(0xFFFFFFFF)
    val textDark = Color(0xFF484848)
    val textLight = Color(0xFF767676)
    val successGreen = Color(0xFF4CAF50)

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
                .padding(16.dp)
        ) {
            Text(
                text = "Security & Privacy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textDark,
                modifier = Modifier.padding(8.dp)
            )

            // Face Verification Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                airbnbRed.copy(alpha = 0.1f),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            tint = airbnbRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Face Verification (2FA)",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = textDark
                        )
                        Text(
                            text = "Add an extra layer of security",
                            style = MaterialTheme.typography.bodySmall,
                            color = textLight
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Switch(
                    checked = faceVerificationEnabled,
                    onCheckedChange = onToggleFaceVerification,
                    enabled = !isDeleting,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = successGreen,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.Gray.copy(alpha = 0.4f)
                    )
                )
            }

            // Divider
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                color = Color.Gray.copy(alpha = 0.2f)
            )

            // TOTP Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                airbnbRed.copy(alpha = 0.1f),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = airbnbRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "TOTP (2FA)",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = textDark
                        )
                        Text(
                            text = "Use Google Authenticator or other TOTP apps",
                            style = MaterialTheme.typography.bodySmall,
                            color = textLight
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Switch(
                    checked = totpEnabled,
                    onCheckedChange = onToggleTotp,
                    enabled = !isSettingUpTotp && !isDisablingTotp,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = successGreen,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.Gray.copy(alpha = 0.4f)
                    )
                )
            }
        }
    }
}

data class SettingsItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
) 