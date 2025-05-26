package com.jvn.myapplication.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    val teal = Color(0xFF008C9E)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isLoginMode) "Login" else "Register",
                    style = MaterialTheme.typography.headlineMedium,
                    color = teal
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (isLoginMode) {
                    LoginForm(
                        authViewModel = authViewModel,
                        uiState = uiState,
                        tealColor = teal
                    )
                } else {
                    RegisterForm(
                        authViewModel = authViewModel,
                        uiState = uiState,
                        tealColor = teal
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = {
                        isLoginMode = !isLoginMode
                        authViewModel.clearMessages()
                    }
                ) {
                    Text(
                        if (isLoginMode) "Don't have an account? Register" else "Already have an account? Login",
                        color = teal
                    )
                }
            }
        }
    }
}