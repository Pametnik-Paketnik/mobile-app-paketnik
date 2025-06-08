// File: ui/unlock/UnlockHistoryScreen.kt
package com.jvn.myapplication.ui.unlock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jvn.myapplication.data.model.UnlockHistoryWithUser
import com.jvn.myapplication.data.repository.BoxRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockHistoryScreen(
    hostId: Int? = null,
    onBack: () -> Unit
) {
    // Airbnb-style color palette
    val airbnbRed = Color(0xFFFF5A5F)
    val lightGray = Color(0xFFF7F7F7)
    val cardWhite = Color(0xFFFFFFFF)
    val textDark = Color(0xFF484848)
    val textLight = Color(0xFF767676)

    val context = LocalContext.current
    val boxRepository = remember { BoxRepository(context) }

    val viewModel: UnlockHistoryViewModel = viewModel {
        UnlockHistoryViewModel(boxRepository, hostId)
    }

    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightGray)
    ) {
        // Enhanced Top App Bar with Airbnb styling
        TopAppBar(
            title = {
                Text(
                    "Unlock History",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            actions = {
                IconButton(onClick = { viewModel.loadUnlockHistory() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = airbnbRed,
                titleContentColor = Color.White
            )
        )

        // Statistics Card with Airbnb styling
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(airbnbRed)
                    .padding(20.dp)
            ) {
                if (!uiState.isLoading) {
                    val totalAttempts = uiState.unlockHistory.size
                    val successfulAttempts = uiState.unlockHistory.count { it.status == "success" }
                    val successRate = if (totalAttempts > 0) {
                        (successfulAttempts * 100) / totalAttempts
                    } else 0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatisticItem(
                            title = "Total Attempts",
                            value = totalAttempts.toString(),
                            icon = Icons.Default.Refresh
                        )
                        StatisticItem(
                            title = "Successful",
                            value = successfulAttempts.toString(),
                            icon = Icons.Default.CheckCircle
                        )
                        StatisticItem(
                            title = "Success Rate",
                            value = "$successRate%",
                            icon = Icons.Default.CheckCircle
                        )
                    }
                }
            }
        }

        // Content Area
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = airbnbRed,
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Loading unlock history...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = textLight
                        )
                    }
                }
            }

            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = airbnbRed.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = airbnbRed,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Error loading data",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = airbnbRed
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                uiState.errorMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = textDark
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    viewModel.clearError()
                                    viewModel.loadUnlockHistory()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = airbnbRed)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }

            uiState.unlockHistory.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "📦",
                                style = MaterialTheme.typography.displayMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No unlock history found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = textDark
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (hostId != null) 
                                    "Box access attempts for your boxes will appear here"
                                else 
                                    "Box access attempts will appear here",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = textLight
                            )
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.unlockHistory) { item ->
                        UnlockHistoryCard(
                            item = item,
                            airbnbRed = airbnbRed,
                            cardWhite = cardWhite,
                            textDark = textDark,
                            textLight = textLight
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatisticItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun UnlockHistoryCard(
    item: UnlockHistoryWithUser,
    airbnbRed: Color,
    cardWhite: Color,
    textDark: Color,
    textLight: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Icon with Airbnb styling
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (item.status == "success")
                        Color(0xFF4CAF50).copy(alpha = 0.1f)
                    else
                        airbnbRed.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.status == "success")
                            Icons.Default.CheckCircle
                        else
                            Icons.Default.Warning,
                        contentDescription = item.status,
                        tint = if (item.status == "success") Color(0xFF4CAF50) else airbnbRed,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Box ${item.boxId}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = textDark
                    )
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (item.status == "success")
                                Color(0xFF4CAF50).copy(alpha = 0.1f)
                            else
                                airbnbRed.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            item.status.uppercase(),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (item.status == "success") Color(0xFF4CAF50) else airbnbRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "User: ${item.username ?: "Unknown"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textLight,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Format: ${item.tokenFormat}",
                        style = MaterialTheme.typography.bodySmall,
                        color = textLight
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    item.timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = textLight
                )
            }
        }
    }
}