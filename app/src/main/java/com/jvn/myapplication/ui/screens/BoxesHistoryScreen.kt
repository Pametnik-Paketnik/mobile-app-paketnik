package com.jvn.myapplication.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jvn.myapplication.data.repository.AuthRepository
import com.jvn.myapplication.ui.unlock.UnlockHistoryScreen
import com.jvn.myapplication.ui.reservations.ReservationsScreen
import com.jvn.myapplication.ui.host.HostReservationsScreen
import com.jvn.myapplication.ui.host.HostBoxesScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxesHistoryScreen() {
    // Airbnb-style color palette
    val airbnbRed = Color(0xFFFF5A5F)
    val darkGray = Color(0xFF484848)
    val lightGray = Color(0xFFF7F7F7)
    val cardWhite = Color(0xFFFFFFFF)
    val textDark = Color(0xFF484848)
    val textLight = Color(0xFF767676)

    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }

    // State for navigation between different views
    var selectedView by remember { mutableStateOf("overview") }
    var isContentVisible by remember { mutableStateOf(false) }

    // User data
    val userId by authRepository.getUserId().collectAsState(initial = null)
    val userType by authRepository.getUserType().collectAsState(initial = null)

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200)
        isContentVisible = true
    }

    // Handle different views
    when (selectedView) {
        "unlock_history" -> {
            userId?.let { id ->
                UnlockHistoryScreen(
                    hostId = id.toIntOrNull(),
                    onBack = { selectedView = "overview" }
                )
            }
            return
        }
        "host_reservations" -> {
            userId?.let { id ->
                HostReservationsScreen(
                    hostId = id.toIntOrNull() ?: 0,
                    onBack = { selectedView = "overview" }
                )
            }
            return
        }
        "host_boxes" -> {
            userId?.let { id ->
                HostBoxesScreen(
                    hostId = id.toIntOrNull() ?: 0,
                    onBack = { selectedView = "overview" }
                )
            }
            return
        }
        "reservations" -> {
            ReservationsScreen()
            return
        }
    }

    // Main overview screen - different content based on user type
    if (userType == "USER") {
        // For regular users, show reservations directly
        ReservationsScreen()
    } else {
        // For HOST users, show the existing overview with unlock history
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(lightGray)
        ) {
            // Clean header with solid color
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Solid background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(airbnbRed)
                )

                // Header content
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
                            // Horizontal layout: icon next to text
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Host Dashboard",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Manage your boxes and monitor activity",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Content area for HOST users
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // All Reservations Card (for HOST users)
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
                        shape = RoundedCornerShape(20.dp),
                        onClick = { selectedView = "host_reservations" }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(
                                        airbnbRed.copy(alpha = 0.1f),
                                        RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = airbnbRed,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(20.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    "All Reservations",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = textDark
                                )
                                Text(
                                    "Manage reservations for your boxes",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textLight
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = textLight,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // All Boxes Card (for HOST users)
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
                        onClick = { selectedView = "host_boxes" }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(
                                        darkGray.copy(alpha = 0.1f),
                                        RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = null,
                                    tint = darkGray,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(20.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    "My Boxes",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = textDark
                                )
                                Text(
                                    "View and manage your boxes",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textLight
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = textLight,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Unlock History Card (for HOST users)
                AnimatedVisibility(
                    visible = isContentVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(800, delayMillis = 300)
                    ) + fadeIn(animationSpec = tween(800, delayMillis = 300))
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(6.dp, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = cardWhite),
                        shape = RoundedCornerShape(20.dp),
                        onClick = { selectedView = "unlock_history" }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(
                                        airbnbRed.copy(alpha = 0.1f),
                                        RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = airbnbRed,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(20.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    "Unlock History",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = textDark
                                )
                                Text(
                                    "Monitor unlock activity for your boxes",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textLight
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        airbnbRed.copy(alpha = 0.2f),
                                        RoundedCornerShape(6.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "HOST",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = airbnbRed
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
} 