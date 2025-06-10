package com.jvn.myapplication.ui.reservations

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jvn.myapplication.data.model.Reservation
import com.jvn.myapplication.data.repository.AuthRepository
import com.jvn.myapplication.data.repository.ReservationRepository
import com.jvn.myapplication.data.repository.BoxRepository
import com.jvn.myapplication.ui.main.QRCodeScanner
import com.jvn.myapplication.ui.screens.OpenBoxScreen
import com.jvn.myapplication.ui.screens.OrderHistoryScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

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
    val boxRepository = remember { BoxRepository(context) }

    // UserBoxOpenViewModel for QR scanning and box opening
    val userBoxOpenViewModel: UserBoxOpenViewModel = viewModel {
        UserBoxOpenViewModel(boxRepository, reservationRepository, context)
    }

    // State for animations
    var isContentVisible by remember { mutableStateOf(false) }
    
    // State for QR scanning
    var isScanningActive by remember { mutableStateOf(false) }
    var scanningReservation by remember { mutableStateOf<Reservation?>(null) }
    var scanningAction by remember { mutableStateOf<UserBoxOpenViewModel.BoxAction?>(null) }
    
    // State for Open Box screen navigation (legacy)
    var showOpenBoxScreen by remember { mutableStateOf(false) }
    var selectedReservationId by remember { mutableStateOf<Int?>(null) }
    
    // State for Extra Orders screen navigation
    var showExtraOrdersScreen by remember { mutableStateOf(false) }
    var selectedReservation by remember { mutableStateOf<Reservation?>(null) }
    
    // State for Order History screen navigation
    var showOrderHistoryScreen by remember { mutableStateOf(false) }
    
    // State for filtering
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, CHECKED_IN, PENDING, CHECKED_OUT

    // User data
    val userId by authRepository.getUserId().collectAsState(initial = null)

    // UI state for box opening
    val userBoxOpenUiState by userBoxOpenViewModel.uiState.collectAsState()

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isScanningActive = true
        } else {
            Toast.makeText(context, "Camera permission required for QR scanning", Toast.LENGTH_LONG).show()
        }
    }

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

    // Functions for handling check-in/check-out with QR scanning
    fun handleCheckIn(reservation: Reservation) {
        scanningReservation = reservation
        scanningAction = UserBoxOpenViewModel.BoxAction.CheckIn
        userBoxOpenViewModel.resetState()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            isScanningActive = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun handleCheckOut(reservation: Reservation) {
        scanningReservation = reservation
        scanningAction = UserBoxOpenViewModel.BoxAction.CheckOut
        userBoxOpenViewModel.resetState()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            isScanningActive = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    // Filter and sort reservations
    val filteredAndSortedReservations = remember(uiState.reservations, selectedFilter) {
        val filtered = when (selectedFilter) {
            "CHECKED_IN" -> uiState.reservations.filter { it.status.uppercase() == "CHECKED_IN" }
            "PENDING" -> uiState.reservations.filter { it.status.uppercase() == "PENDING" }
            "CHECKED_OUT" -> uiState.reservations.filter { it.status.uppercase() == "CHECKED_OUT" }
            else -> uiState.reservations // ALL
        }
        
        // Sort by priority: CHECKED_IN first, PENDING second, CHECKED_OUT last
        filtered.sortedWith(compareBy { reservation ->
            when (reservation.status.uppercase()) {
                "CHECKED_IN" -> 1
                "PENDING" -> 2
                "CHECKED_OUT" -> 3
                else -> 4
            }
        })
    }

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

    // Handle successful check-in/check-out completion
    LaunchedEffect(userBoxOpenUiState.isBoxOpened, userBoxOpenUiState.successMessage) {
        if (userBoxOpenUiState.isBoxOpened && userBoxOpenUiState.successMessage != null) {
            Toast.makeText(context, userBoxOpenUiState.successMessage, Toast.LENGTH_LONG).show()
            kotlinx.coroutines.delay(2000)
            // Reload reservations to update the status
            viewModel?.loadReservations()
            userBoxOpenViewModel.resetState()
            scanningReservation = null
            scanningAction = null
            isScanningActive = false
        }
    }

    // Handle box opening errors
    LaunchedEffect(userBoxOpenUiState.errorMessage) {
        if (userBoxOpenUiState.errorMessage != null) {
            Toast.makeText(context, userBoxOpenUiState.errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    // QR Scanner UI - Full screen when active
    if (isScanningActive && scanningReservation != null) {
        AnimatedVisibility(
            visible = true,
            enter = scaleIn() + fadeIn()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(48.dp))

                // Header card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardWhite),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (scanningAction == UserBoxOpenViewModel.BoxAction.CheckIn) 
                                Icons.Default.Done else Icons.Default.ExitToApp,
                            contentDescription = null,
                            tint = airbnbRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Scan Box QR Code",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = textDark
                        )
                        Text(
                            text = "Scan the QR code on box #${scanningReservation!!.box?.boxId} to ${if (scanningAction == UserBoxOpenViewModel.BoxAction.CheckIn) "check in" else "check out"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textLight,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Large camera view for better scanning
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    QRCodeScanner(
                        onQrCodeScanned = { scannedData ->
                            try {
                                val boxId = scannedData.toIntOrNull()
                                if (boxId != null && scanningReservation != null && scanningAction != null) {
                                    println("🔍 DEBUG - ReservationsScreen: QR scanned - Box ID: $boxId")
                                    isScanningActive = false
                                    when (scanningAction) {
                                        UserBoxOpenViewModel.BoxAction.CheckIn -> {
                                            userBoxOpenViewModel.startCheckIn(scanningReservation!!, boxId)
                                        }
                                        UserBoxOpenViewModel.BoxAction.CheckOut -> {
                                            userBoxOpenViewModel.startCheckOut(scanningReservation!!, boxId)
                                        }
                                        null -> { /* Do nothing */ }
                                    }
                                } else {
                                    Toast.makeText(context, "Invalid QR code. Please scan a valid box QR code.", Toast.LENGTH_LONG).show()
                                    isScanningActive = false
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error processing QR code: ${e.message}", Toast.LENGTH_LONG).show()
                                isScanningActive = false
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Cancel button
                Button(
                    onClick = { 
                        isScanningActive = false
                        scanningReservation = null
                        scanningAction = null
                        userBoxOpenViewModel.clearMessages()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = airbnbRed),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel Scanning", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        return
    }

    // Box opening confirmation dialog (YES/NO)
    if (userBoxOpenUiState.showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { /* Don't allow dismissal during audio playback */ },
            title = null,
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Sound wave icon
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = airbnbRed,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Box Opening Signal Sent! 📦",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = textDark,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Listen to the confirmation sound and check if the box opened physically.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = textLight,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Did the box open successfully?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textDark,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Yes/No buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // NO button
                        Button(
                            onClick = {
                                userBoxOpenViewModel.confirmBoxOpening(false)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "NO",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        // YES button
                        Button(
                            onClick = {
                                userBoxOpenViewModel.confirmBoxOpening(true)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "YES",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
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

        Spacer(modifier = Modifier.height(16.dp))
        
        // Filter chips
        AnimatedVisibility(
            visible = isContentVisible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(600, delayMillis = 200)
            ) + fadeIn(animationSpec = tween(600, delayMillis = 200))
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                item {
                    ReservationFilterChip(
                        text = "All",
                        isSelected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        count = uiState.reservations.size
                    )
                }
                item {
                    ReservationFilterChip(
                        text = "Active",
                        isSelected = selectedFilter == "CHECKED_IN",
                        onClick = { selectedFilter = "CHECKED_IN" },
                        count = uiState.reservations.count { it.status.uppercase() == "CHECKED_IN" }
                    )
                }
                item {
                    ReservationFilterChip(
                        text = "Pending",
                        isSelected = selectedFilter == "PENDING",
                        onClick = { selectedFilter = "PENDING" },
                        count = uiState.reservations.count { it.status.uppercase() == "PENDING" }
                    )
                }
                item {
                    ReservationFilterChip(
                        text = "Completed",
                        isSelected = selectedFilter == "CHECKED_OUT",
                        onClick = { selectedFilter = "CHECKED_OUT" },
                        count = uiState.reservations.count { it.status.uppercase() == "CHECKED_OUT" }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
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
                    
                    !uiState.errorMessage.isNullOrEmpty() -> {
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
                                "Error loading reservations",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = textDark
                            )
                            Text(
                                uiState.errorMessage!!,
                                style = MaterialTheme.typography.bodyLarge,
                                color = textLight,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    filteredAndSortedReservations.isEmpty() && !uiState.isLoading -> {
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
                                text = if (selectedFilter == "ALL") "No reservations found" else "No ${selectedFilter.lowercase().replace("_", " ")} reservations",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = textDark
                            )
                            Text(
                                text = if (selectedFilter == "ALL") 
                                    "Your box reservations will appear here" 
                                else 
                                    "No reservations match the selected filter",
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
                            items(filteredAndSortedReservations) { reservation ->
                                ReservationCard(
                                    reservation = reservation,
                                    allReservations = uiState.reservations,
                                    onCheckIn = { handleCheckIn(reservation) },
                                    onCheckOut = { handleCheckOut(reservation) },
                                    onOpenBox = { reservationId ->
                                        selectedReservationId = reservationId
                                        showOpenBoxScreen = true
                                    },
                                    onOrderItems = { 
                                        selectedReservation = reservation
                                        showExtraOrdersScreen = true
                                    },
                                    onOrderHistory = { reservationId ->
                                        selectedReservationId = reservationId
                                        showOrderHistoryScreen = true
                                    },
                                    isCheckingIn = uiState.checkingInReservations.contains(reservation.id) || 
                                                   (userBoxOpenUiState.isLoading && scanningReservation?.id == reservation.id && scanningAction == UserBoxOpenViewModel.BoxAction.CheckIn),
                                    isCheckingOut = uiState.checkingOutReservations.contains(reservation.id) || 
                                                    (userBoxOpenUiState.isLoading && scanningReservation?.id == reservation.id && scanningAction == UserBoxOpenViewModel.BoxAction.CheckOut)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Legacy Open Box Screen overlay - can be removed if not needed elsewhere
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
    
    // Order History Screen overlay
    if (showOrderHistoryScreen && selectedReservationId != null) {
        OrderHistoryScreen(
            reservationId = selectedReservationId!!,
            onBackClick = {
                showOrderHistoryScreen = false
                selectedReservationId = null
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
    onOrderHistory: (Int) -> Unit,
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
                                onCheckIn()
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
                                    text = if (shouldDisableCheckIn) "CHECK IN" else "CHECK IN",
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
                    // Order-related actions section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Order Actions",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = textLight,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
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
                            
                            // Order History button for CHECKED_IN
                            OutlinedButton(
                                onClick = { onOrderHistory(reservation.id) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF9C27B0)
                                ),
                                border = BorderStroke(1.dp, Color(0xFF9C27B0)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.List,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "ORDER HISTORY",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp)) // Large spacing to separate sections
                    
                    // Check Out section - Primary action
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)), // Light blue background
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Ready to Leave?",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1976D2),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Button(
                                onClick = onCheckOut,
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
                
                "CHECKED_OUT" -> {
                    // Order Actions section for checked out reservations - same style as CHECKED_IN
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)), // Same light gray as CHECKED_IN
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Order Actions",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = textLight,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            // Only Order History button for CHECKED_OUT
                            OutlinedButton(
                                onClick = { onOrderHistory(reservation.id) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF9C27B0)
                                ),
                                border = BorderStroke(1.dp, Color(0xFF9C27B0)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.List,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "ORDER HISTORY",
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
}

@Composable
private fun StatusChip(status: String) {
    val (backgroundColor, textColor) = when (status.lowercase()) {
        "checked_in" -> Color(0xFF4CAF50).copy(alpha = 0.1f) to Color(0xFF4CAF50) // Same green as ORDER ITEMS
        "checked_out" -> Color(0xFF2196F3).copy(alpha = 0.1f) to Color(0xFF2196F3) // Same blue as CHECK OUT
        "pending" -> Color(0xFFFF9800).copy(alpha = 0.1f) to Color(0xFFFF9800)
        "cancelled" -> Color(0xFFF44336).copy(alpha = 0.1f) to Color(0xFFF44336)
        "completed" -> Color(0xFF2196F3).copy(alpha = 0.1f) to Color(0xFF2196F3)
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

@Composable
private fun ReservationFilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    count: Int
) {
    val airbnbRed = Color(0xFFFF5A5F)
    val textDark = Color(0xFF484848)
    val textLight = Color(0xFF767676)
    val cardWhite = Color(0xFFFFFFFF)
    
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
                if (count > 0) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) cardWhite.copy(alpha = 0.8f) else airbnbRed.copy(alpha = 0.1f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) airbnbRed else textDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = cardWhite,
            labelColor = textDark,
            selectedContainerColor = airbnbRed,
            selectedLabelColor = cardWhite
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = if (isSelected) airbnbRed else textLight.copy(alpha = 0.5f),
            selectedBorderColor = airbnbRed
        )
    )
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