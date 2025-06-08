package com.jvn.myapplication.ui.host

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jvn.myapplication.data.model.Reservation
import com.jvn.myapplication.data.repository.ReservationRepository
import com.jvn.myapplication.ui.reservations.ReservationsUiState
import com.jvn.myapplication.ui.reservations.ReservationsViewModel
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostReservationsScreen(
    hostId: Int,
    onBack: () -> Unit
) {
    // Airbnb-style color palette
    val airbnbRed = Color(0xFFFF5A5F)
    val lightGray = Color(0xFFF7F7F7)
    val cardWhite = Color(0xFFFFFFFF)
    val textDark = Color(0xFF484848)
    val textLight = Color(0xFF767676)

    val context = LocalContext.current
    val reservationRepository = remember { ReservationRepository(context) }

    // State for animations
    var isContentVisible by remember { mutableStateOf(false) }
    var reservations by remember { mutableStateOf<List<Reservation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(hostId) {
        isLoading = true
        errorMessage = null
        
        try {
            println("🔍 DEBUG - HostReservationsScreen: Loading reservations for host $hostId")
            
            // Actually call the API now instead of using empty list
            reservationRepository.getReservationsByHost(hostId)
                .onSuccess { loadedReservations ->
                    println("🔍 DEBUG - HostReservationsScreen: Loaded ${loadedReservations.size} reservations")
                    reservations = loadedReservations
                    errorMessage = null
                }
                .onFailure { exception ->
                    println("🔍 DEBUG - HostReservationsScreen: Error loading reservations: ${exception.message}")
                    errorMessage = exception.message ?: "Failed to load reservations"
                    reservations = emptyList()
                }
        } catch (e: Exception) {
            println("🔍 DEBUG - HostReservationsScreen: Exception: ${e.message}")
            errorMessage = e.message
            reservations = emptyList()
        } finally {
            isLoading = false
        }
        
        kotlinx.coroutines.delay(200)
        isContentVisible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightGray)
    ) {
        // Header with back button
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
                    .padding(16.dp)
            ) {
                // Back button
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                AnimatedVisibility(
                    visible = isContentVisible,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(animationSpec = tween(600))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Horizontal layout: icon next to text
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Host Reservations",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Manage reservations for your boxes",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Content area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when {
                isLoading -> {
                    // Loading state
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = airbnbRed)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Loading reservations...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = textLight
                        )
                    }
                }
                
                errorMessage != null -> {
                    // Error state
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = airbnbRed,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            errorMessage!!,
                            style = MaterialTheme.typography.bodyLarge,
                            color = textDark,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                reservations.isEmpty() && !isLoading -> {
                    // Empty state
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = textLight,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No reservations found",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = textDark
                        )
                        Text(
                            "Reservations for your boxes will appear here",
                            style = MaterialTheme.typography.bodyLarge,
                            color = textLight,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                else -> {
                    // Reservations list
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(reservations) { reservation ->
                            HostReservationCard(reservation = reservation)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HostReservationCard(
    reservation: Reservation
) {
    // Airbnb-style colors
    val cardWhite = Color(0xFFFFFFFF)
    val textDark = Color(0xFF484848)
    val textLight = Color(0xFF767676)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = cardWhite),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            // Reservation header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = reservation.box?.boxId?.let { "Box $it" } ?: "Box ${reservation.id}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = textDark
                    )
                    Text(
                        text = "Guest: ${reservation.guest?.username ?: "Unknown"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = textLight
                    )
                }
                
                StatusChip(status = reservation.status)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Reservation details
            reservation.box?.location?.let { location ->
                DetailRow(
                    icon = Icons.Default.LocationOn,
                    label = "Location",
                    value = location
                )
            }
            
            DetailRow(
                icon = Icons.Default.DateRange,
                label = "Check-in",
                value = formatDate(reservation.checkinAt)
            )
            
            DetailRow(
                icon = Icons.Default.DateRange,
                label = "Check-out",
                value = formatDate(reservation.checkoutAt)
            )
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val (backgroundColor, textColor) = when (status.lowercase()) {
        "pending" -> Color(0xFFFF9800).copy(alpha = 0.1f) to Color(0xFFFF9800)
        "checked_in" -> Color(0xFF4CAF50).copy(alpha = 0.1f) to Color(0xFF4CAF50)
        "checked_out" -> Color(0xFF2196F3).copy(alpha = 0.1f) to Color(0xFF2196F3)
        "cancelled" -> Color(0xFFF44336).copy(alpha = 0.1f) to Color(0xFFF44336)
        else -> Color(0xFF757575).copy(alpha = 0.1f) to Color(0xFF757575)
    }
    
    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = status.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    val textLight = Color(0xFF767676)
    val textDark = Color(0xFF484848)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textLight,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = textLight,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = textDark,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
        val outputFormat = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
        val date = inputFormat.parse(dateString)
        outputFormat.format(date ?: java.util.Date())
    } catch (e: Exception) {
        dateString
    }
} 