package com.jvn.myapplication.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import androidx.compose.ui.platform.LocalContext
import com.jvn.myapplication.data.repository.FaceAuthRepository
import com.jvn.myapplication.ui.face.LoginFaceVerificationScreen
import com.jvn.myapplication.workers.LoginApprovalWorker
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginApprovalScreen(
    pendingAuthId: String,
    username: String,
    ip: String,
    location: String,
    faceAuthRepository: FaceAuthRepository,
    onApprovalComplete: () -> Unit,
    onDeny: () -> Unit
) {
    val context = LocalContext.current
    var showFaceVerification by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    if (showFaceVerification) {
        // Use the same single-photo verification as login
        LoginFaceVerificationScreen(
            faceAuthRepository = faceAuthRepository,
            onVerificationSuccess = {
                // Face verification successful - approve the login
                isProcessing = true
                val approveWork = OneTimeWorkRequestBuilder<LoginApprovalWorker>()
                    .setInputData(workDataOf(
                        "pendingAuthId" to pendingAuthId,
                        "action" to "approve"
                    ))
                    .build()
                
                WorkManager.getInstance(context).enqueue(approveWork)
                onApprovalComplete()
            },
            onLogout = {
                // User chooses to deny instead of verifying
                onDeny()
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F7F7))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Security Icon
            Card(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(40.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF008C9E))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Security",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = "🔐 Login Approval Required",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Login Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Someone is trying to log in to your account:",
                        fontSize = 16.sp,
                        color = Color(0xFF7F8C8D),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Username
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User",
                            tint = Color(0xFF008C9E),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Username",
                                fontSize = 12.sp,
                                color = Color(0xFF7F8C8D)
                            )
                            Text(
                                text = username,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF2C3E50)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // IP Address
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = Color(0xFF008C9E),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "IP Address",
                                fontSize = 12.sp,
                                color = Color(0xFF7F8C8D)
                            )
                            Text(
                                text = ip,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF2C3E50)
                            )
                        }
                    }

                    if (location != "Unknown location") {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Spacer(modifier = Modifier.width(32.dp))
                            Text(
                                text = "Location: $location",
                                fontSize = 14.sp,
                                color = Color(0xFF7F8C8D)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isProcessing) {
                CircularProgressIndicator(
                    color = Color(0xFF008C9E),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Processing approval...",
                    fontSize = 16.sp,
                    color = Color(0xFF7F8C8D)
                )
            } else {
                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Approve Button
                    Button(
                        onClick = { showFaceVerification = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF27AE60)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "✓ Approve with Face Verification",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Deny Button
                    OutlinedButton(
                        onClick = {
                            isProcessing = true
                            val denyWork = OneTimeWorkRequestBuilder<LoginApprovalWorker>()
                                .setInputData(workDataOf(
                                    "pendingAuthId" to pendingAuthId,
                                    "action" to "deny"
                                ))
                                .build()
                            
                            WorkManager.getInstance(context).enqueue(denyWork)
                            onDeny()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFE74C3C)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "✗ Deny Login",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "If this wasn't you, deny the request and consider changing your password.",
                fontSize = 12.sp,
                color = Color(0xFF7F8C8D),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
} 