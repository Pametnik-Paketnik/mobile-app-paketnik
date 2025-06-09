package com.jvn.myapplication.ui.reservations

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
import androidx.compose.runtime.collectAsState
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
import com.jvn.myapplication.data.repository.AuthRepository
import com.jvn.myapplication.data.repository.ReservationRepository
import com.jvn.myapplication.ui.screens.OpenBoxScreen
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationsScreen() {
    // Airbnb-style color palette
    val airbnbRed = Color(0xFFFF5A5F)
    val lightGray = Color(0xFFF7F7F7)
    val cardWhite = Color(0xFFFFFFFF)
    val textDark = Color(0xFF484848)
    val textLight = Color(0xFF767676)

    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val reservationRepository = remember { ReservationRepository(context) }

    // State for animations
    var isContentVisible by remember { mutableStateOf(false) }
    
    // State for Open Box screen navigation
    var showOpenBoxScreen by remember { mutableStateOf(false) }
    var selectedReservationId by remember { mutableStateOf<Int?>(null) }
    
    // State for Extra Orders screen navigation
    var showExtraOrdersScreen by remember { mutableStateOf(false) }
    var selectedReservation by remember { mutableStateOf<Reservation?>(null) }

    // User data
    val userId by authRepository.getUserId().collectAsState(initial = null)

    // Debug logging
    LaunchedEffect(userId) {
        val currentUserId = userId
        println("🔍 DEBUG - ReservationsScreen: User ID received: '$currentUserId'")
        println("🔍 DEBUG - ReservationsScreen: User ID type: ${currentUserId?.javaClass?.simpleName}")
        println("🔍 DEBUG - ReservationsScreen: User ID isEmpty: ${currentUserId.isNullOrEmpty()}")
        
        // Add more detailed checks
        if (currentUserId != null) {
            println("🔍 DEBUG - ReservationsScreen: User ID length: ${currentUserId.length}")
            println("🔍 DEBUG - ReservationsScreen: User ID contents: '$currentUserId'")
            val asInt = currentUserId.toIntOrNull()
            println("🔍 DEBUG - ReservationsScreen: User ID as int: $asInt")
        }
    }

    // Only create ViewModel when we have a valid user ID
    val viewModel: ReservationsViewModel? = remember(userId) {
        val currentUserId = userId
        if (!currentUserId.isNullOrEmpty()) {
            println("🔍 DEBUG - ReservationsScreen: Creating ViewModel with user ID: '$currentUserId'")
            ReservationsViewModel(reservationRepository, currentUserId)
        } else {
            println("🔍 DEBUG - ReservationsScreen: Not creating ViewModel - user ID is null or empty")
            null
        }
    }

    val uiState by (viewModel?.uiState ?: MutableStateFlow(ReservationsUiState())).collectAsState()

    LaunchedEffect(viewModel) {
        if (viewModel != null) {
            println("🔍 DEBUG - ReservationsScreen: ViewModel created, loading reservations")
            println("🔍 DEBUG - ReservationsScreen: About to call viewModel.loadReservations()")
            viewModel.loadReservations()
            println("🔍 DEBUG - ReservationsScreen: Called viewModel.loadReservations()")
        } else {
            val currentUserId = userId
            println("🔍 DEBUG - ReservationsScreen: ViewModel is null, not loading reservations")
            println("🔍 DEBUG - ReservationsScreen: Current userId: '$currentUserId'")
        }
        kotlinx.coroutines.delay(200)
        isContentVisible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightGray)
    ) {
        // Header with consistent styling
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
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "My Reservations",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "View and manage your box reservations",
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
            // Add debug info at the top
            val currentUserId = userId
            if (currentUserId != null) {
                Text(
                    "DEBUG: UserId='$currentUserId', ViewModel=${if (viewModel != null) "Created" else "NULL"}, " +
                    "Loading=${uiState.isLoading}, Reservations=${uiState.reservations.size}, " +
                    "Error='${uiState.errorMessage}'",
                    style = MaterialTheme.typography.bodySmall,
                    color = airbnbRed,
                    modifier = Modifier.padding(8.dp)
                )
            }
            
            Column(
                modifier = Modifier.fillMaxSize().padding(top = if (currentUserId != null) 60.dp else 0.dp)
            ) {
                when {
                    uiState.isLoading -> {
                        // Loading state
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = airbnbRed)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Loading your reservations...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = textLight
                            )
                        }
                    }
                    
                    uiState.errorMessage != null -> {
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
                                uiState.errorMessage!!,
                                style = MaterialTheme.typography.bodyLarge,
                                color = textDark,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel?.loadReservations() ?: Unit },
                                colors = ButtonDefaults.buttonColors(containerColor = airbnbRed)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                    
                    uiState.reservations.isEmpty() && !uiState.isLoading -> {
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
                                "Your box reservations will appear here",
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
                            items(uiState.reservations) { reservation ->
                                ReservationCard(
                                    reservation = reservation,
                                    allReservations = uiState.reservations,
                                    onCheckIn = { viewModel?.checkIn(reservation.id) ?: Unit },
                                    onCheckOut = { viewModel?.checkOut(reservation.id) ?: Unit },
                                    onOpenBox = { reservationId ->
                                        selectedReservationId = reservationId
                                        showOpenBoxScreen = true
                                    },
                                    onOrderItems = { 
                                        selectedReservation = reservation
                                        showExtraOrdersScreen = true
                                    },
                                    isCheckingIn = uiState.checkingInReservations.contains(reservation.id),
                                    isCheckingOut = uiState.checkingOutReservations.contains(reservation.id)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Open Box Screen overlay
    if (showOpenBoxScreen && selectedReservationId != null) {
        OpenBoxScreen(
            reservationId = selectedReservationId!!,
            onBackClick = {
                showOpenBoxScreen = false
                selectedReservationId = null
            },
            onSuccess = { qrCode ->
                // Handle successful QR scan
                println("🔍 DEBUG - QR Code scanned: $qrCode")
                
                // Determine which action to take based on reservation status
                val reservation = uiState.reservations.find { it.id == selectedReservationId }
                if (reservation != null) {
                    when (reservation.status.uppercase()) {
                        "PENDING" -> {
                            viewModel?.checkIn(selectedReservationId!!)
                        }
                        "CHECKED_IN" -> {
                            viewModel?.checkOut(selectedReservationId!!)
                        }
                    }
                }
                
                // Close the screen after a delay (handled by OpenBoxScreen)
            }
        )
    }
    
    // Extra Orders Screen overlay
    if (showExtraOrdersScreen && selectedReservation != null) {
        ExtraOrdersScreen(
            reservation = selectedReservation!!,
            onBackClick = {
                showExtraOrdersScreen = false
                selectedReservation = null
            }
        )
    }
}

@Composable
private fun ReservationCard(
    reservation: Reservation,
    allReservations: List<Reservation>,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit,
    onOpenBox: (Int) -> Unit,
    onOrderItems: () -> Unit,
    isCheckingIn: Boolean,
    isCheckingOut: Boolean
) {
    // Airbnb-style colors
    val airbnbRed = Color(0xFFFF5A5F)
    val cardWhite = Color(0xFFFFFFFF)
    val textDark = Color(0xFF484848)
    val textLight = Color(0xFF767676)
    
    // Check if user already has a CHECKED_IN reservation
    val hasCheckedInReservation = allReservations.any { it.status.uppercase() == "CHECKED_IN" }
    val isCurrentPending = reservation.status.uppercase() == "PENDING"
    val shouldDisableCheckIn = hasCheckedInReservation && isCurrentPending

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
                        text = "Reservation #${reservation.id}",
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action buttons based on reservation status
            when (reservation.status.uppercase()) {
                "PENDING" -> {
                    Button(
                        onClick = { 
                            if (!shouldDisableCheckIn) {
                                onOpenBox(reservation.id)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (shouldDisableCheckIn) Color(0xFFE0E0E0) else airbnbRed,
                            disabledContainerColor = Color(0xFFE0E0E0)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isCheckingIn && !shouldDisableCheckIn
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isCheckingIn) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("CHECKING IN...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (shouldDisableCheckIn) Color(0xFF9E9E9E) else Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (shouldDisableCheckIn) "ALREADY CHECKED IN ELSEWHERE" else "CHECK IN",
                                    fontWeight = FontWeight.Bold,
                                    color = if (shouldDisableCheckIn) Color(0xFF9E9E9E) else Color.White
                                )
                            }
                        }
                    }
                    
                    // Show explanation when button is disabled
                    if (shouldDisableCheckIn) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = textLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Check out from your current reservation first",
                                style = MaterialTheme.typography.bodySmall,
                                color = textLight,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                "CHECKED_IN" -> {
                    // Order Items button
                    Button(
                        onClick = onOrderItems,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ORDER ITEMS",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Check Out button
                    Button(
                        onClick = { onOpenBox(reservation.id) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isCheckingOut
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isCheckingOut) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("CHECKING OUT...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "CHECK OUT",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val (backgroundColor, textColor) = when (status.lowercase()) {
        "active" -> Color(0xFF4CAF50).copy(alpha = 0.1f) to Color(0xFF4CAF50)
        "pending" -> Color(0xFFFF9800).copy(alpha = 0.1f) to Color(0xFFFF9800)
        "completed" -> Color(0xFF2196F3).copy(alpha = 0.1f) to Color(0xFF2196F3)
        "cancelled" -> Color(0xFFF44336).copy(alpha = 0.1f) to Color(0xFFF44336)
        else -> Color(0xFF757575).copy(alpha = 0.1f) to Color(0xFF757575)
    }
    
    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = status.capitalize(),
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