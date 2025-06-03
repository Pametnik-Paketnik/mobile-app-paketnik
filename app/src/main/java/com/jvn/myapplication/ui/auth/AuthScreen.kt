package com.jvn.myapplication.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    var isLoginMode by remember { mutableStateOf(true) }

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onAuthSuccess()
        }
    }

    // Airbnb-style color palette
    val airbnbRed = Color(0xFFFF5A5F)
    val lightGray = Color(0xFFF7F7F7)
    val cardWhite = Color(0xFFFFFFFF)
    val textDark = Color(0xFF484848)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightGray)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = cardWhite),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isLoginMode) "Welcome Back" else "Create Account",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = textDark
                )

                Text(
                    text = if (isLoginMode) "Sign in to your account" else "Join Air-Box today",
                    style = MaterialTheme.typography.bodyLarge,
                    color = textDark.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (isLoginMode) {
                    LoginForm(
                        authViewModel = authViewModel,
                        uiState = uiState,
                        airbnbColor = airbnbRed
                    )
                } else {
                    RegisterForm(
                        authViewModel = authViewModel,
                        uiState = uiState,
                        airbnbColor = airbnbRed
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(
                    onClick = {
                        isLoginMode = !isLoginMode
                        authViewModel.clearMessages()
                    }
                ) {
                    Text(
                        if (isLoginMode) "Don't have an account? Register" else "Already have an account? Login",
                        color = airbnbRed,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}