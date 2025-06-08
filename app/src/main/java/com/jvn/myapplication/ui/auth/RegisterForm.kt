package com.jvn.myapplication.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun RegisterForm(
    authViewModel: AuthViewModel,
    uiState: AuthUiState,
    airbnbColor: Color
) {
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedUserType by remember { mutableStateOf("USER") }

    // Airbnb-style colors
    val lightGray = Color(0xFFF7F7F7)
    val textDark = Color(0xFF484848)

    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("First Name") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = airbnbColor,
            focusedLabelColor = airbnbColor
        )
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = surname,
        onValueChange = { surname = it },
        label = { Text("Last Name") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = airbnbColor,
            focusedLabelColor = airbnbColor
        )
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Email") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = airbnbColor,
            focusedLabelColor = airbnbColor
        )
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = airbnbColor,
            focusedLabelColor = airbnbColor
        )
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = confirmPassword,
        onValueChange = { confirmPassword = it },
        label = { Text("Confirm Password") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = airbnbColor,
            focusedLabelColor = airbnbColor
        )
    )

    Spacer(modifier = Modifier.height(24.dp))

    // User Type Selection
    Text(
        text = "Account Type",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = textDark
    )

    Spacer(modifier = Modifier.height(12.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = lightGray
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // USER option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (selectedUserType == "USER"),
                        onClick = { selectedUserType = "USER" },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (selectedUserType == "USER"),
                    onClick = { selectedUserType = "USER" },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = airbnbColor
                    )
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Guest User",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = textDark
                    )
                    Text(
                        text = "Make reservations and access boxes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textDark.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // HOST option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (selectedUserType == "HOST"),
                        onClick = { selectedUserType = "HOST" },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (selectedUserType == "HOST"),
                    onClick = { selectedUserType = "HOST" },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = airbnbColor
                    )
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Box Host",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = textDark
                    )
                    Text(
                        text = "Manage boxes and handle reservations",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textDark.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = { authViewModel.register(name, surname, email, password, confirmPassword, selectedUserType) },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = airbnbColor),
        shape = RoundedCornerShape(16.dp),
        enabled = !uiState.isLoading && name.isNotBlank() && surname.isNotBlank() && 
                email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank()
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White
            )
        } else {
            Text(
                "Create Account",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }

    uiState.successMessage?.let { message ->
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = airbnbColor.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = message,
                color = airbnbColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    uiState.errorMessage?.let { error ->
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}