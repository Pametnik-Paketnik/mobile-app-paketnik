package com.jvn.myapplication.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jvn.myapplication.data.model.TwoFactorMethod

@Composable
fun TwoFactorSelectionScreen(
    availableMethods: List<TwoFactorMethod>,
    onMethodSelected: (String) -> Unit
) {
    val airbnbRed = Color(0xFFFF5A5F)
    val lightGray = Color(0xFFF7F7F7)
    val cardWhite = Color(0xFFFFFFFF)
    val textDark = Color(0xFF484848)
    val textLight = Color(0xFF767676)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightGray)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title
        Text(
            text = "Choose Verification Method",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = textDark,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Select how you want to verify your identity",
            style = MaterialTheme.typography.bodyLarge,
            color = textLight,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Method Cards
        availableMethods.forEach { method ->
            TwoFactorMethodCard(
                method = method,
                onClick = { onMethodSelected(method.type) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TwoFactorMethodCard(
    method: TwoFactorMethod,
    onClick: () -> Unit
) {
    val airbnbRed = Color(0xFFFF5A5F)
    val cardWhite = Color(0xFFFFFFFF)
    val textDark = Color(0xFF484848)
    val textLight = Color(0xFF767676)

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = cardWhite),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        airbnbRed.copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (method.type) {
                        "face_id" -> Icons.Default.Face
                        "totp" -> Icons.Default.Lock
                        else -> Icons.Default.Lock
                    },
                    contentDescription = null,
                    tint = airbnbRed,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = method.display_name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textDark
                )
                Text(
                    text = when (method.type) {
                        "face_id" -> "Use facial recognition to verify your identity"
                        "totp" -> "Enter code from your authenticator app"
                        else -> "Verify your identity"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = textLight
                )
            }

            // Arrow
            Icon(
                imageVector = Icons.Default.Lock, // You can change this to an arrow icon
                contentDescription = null,
                tint = textLight,
                modifier = Modifier.size(20.dp)
            )
        }
    }
} 