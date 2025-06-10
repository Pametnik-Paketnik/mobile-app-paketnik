package com.jvn.myapplication.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jvn.myapplication.data.model.ExtraOrder
import com.jvn.myapplication.data.repository.AuthRepository
import com.jvn.myapplication.data.repository.CleanerRepository
import com.jvn.myapplication.data.repository.BoxRepository
import com.jvn.myapplication.ui.main.QRCodeScanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanerScreen() {
    // Airbnb-style color palette
    val airbnbRed = Color(0xFFFF5A5F)
    val lightGray = Color(0xFFF7F7F7)
    val cardWhite = Color(0xFFFFFFFF)
    val textDark = Color(0xFF484848)
    val textLight = Color(0xFF767676)
    val successGreen = Color(0xFF00C851)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Repositories
    val authRepository = remember { AuthRepository(context) }
    val cleanerRepository = remember { CleanerRepository(context) }
    val boxRepository = remember { BoxRepository(context) }

    // ViewModels
    val cleanerBoxOpenViewModel: CleanerBoxOpenViewModel = viewModel {
        CleanerBoxOpenViewModel(boxRepository, cleanerRepository, context)
    }

    // State variables
    var extraOrders by remember { mutableStateOf<List<ExtraOrder>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isContentVisible by remember { mutableStateOf(false) }
    var selectedOrder by remember { mutableStateOf<ExtraOrder?>(null) }
    var isScanningActive by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }
    var fulfillNotes by remember { mutableStateOf("") }

    // User data from repository
    val name by authRepository.getName().collectAsState(initial = null)
    val userType by authRepository.getUserType().collectAsState(initial = null)

    // UI state for box opening
    val cleanerBoxOpenUiState by cleanerBoxOpenViewModel.uiState.collectAsState()

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isScanningActive = true
        } else {
            Toast.makeText(context, "Camera permission required", Toast.LENGTH_LONG).show()
        }
    }

    // Load extra orders
    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        
        try {
            cleanerRepository.getAllExtraOrders()
                .onSuccess { orders ->
                    extraOrders = orders
                    errorMessage = null
                }
                .onFailure { exception ->
                    errorMessage = exception.message ?: "Failed to load orders"
                    extraOrders = emptyList()
                }
        } catch (e: Exception) {
            errorMessage = e.message
            extraOrders = emptyList()
        } finally {
            isLoading = false
        }
        
        delay(300)
        isContentVisible = true
    }

    // Handle fulfill order - first scan QR, then add notes
    fun handleFulfillOrder(order: ExtraOrder) {
        selectedOrder = order
        fulfillNotes = ""
        cleanerBoxOpenViewModel.resetState()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            isScanningActive = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Handle notes submission after box opening
    fun submitFulfillment() {
        val order = selectedOrder
        val notes = fulfillNotes.trim().takeIf { it.isNotBlank() }
        
        if (order != null) {
            cleanerBoxOpenViewModel.submitFulfillment(order.id, notes)
        }
    }

    // Handle successful box opening and fulfillment completion
    LaunchedEffect(cleanerBoxOpenUiState.isBoxOpened, cleanerBoxOpenUiState.successMessage) {
        if (cleanerBoxOpenUiState.isBoxOpened && cleanerBoxOpenUiState.successMessage != null) {
            Toast.makeText(context, cleanerBoxOpenUiState.successMessage, Toast.LENGTH_LONG).show()
            delay(2000)
            // Reload orders to update the status
            scope.launch {
                cleanerRepository.getAllExtraOrders()
                    .onSuccess { orders ->
                        extraOrders = orders
                    }
            }
            cleanerBoxOpenViewModel.resetState()
            selectedOrder = null
            isScanningActive = false
            showNotesDialog = false
            fulfillNotes = ""
        }
    }

    // Handle box opening errors
    LaunchedEffect(cleanerBoxOpenUiState.errorMessage) {
        if (cleanerBoxOpenUiState.errorMessage != null) {
            Toast.makeText(context, cleanerBoxOpenUiState.errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    // Handle notes dialog visibility
    LaunchedEffect(cleanerBoxOpenUiState.showNotesDialog) {
        showNotesDialog = cleanerBoxOpenUiState.showNotesDialog
    }

    if (isScanningActive && selectedOrder != null) {
        // QR Scanner UI - Full screen camera view
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

                // Header
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
                             imageVector = Icons.Default.Lock,
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
                            text = "Position QR code within the frame to open the box",
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
                                if (boxId != null && selectedOrder != null) {
                                    println("🔍 DEBUG - CleanerScreen: QR scanned - Box ID: $boxId")
                                    isScanningActive = false
                                    cleanerBoxOpenViewModel.openBoxForOrder(boxId, selectedOrder!!)
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

                Button(
                    onClick = { 
                        isScanningActive = false
                        selectedOrder = null
                        cleanerBoxOpenViewModel.clearMessages()
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
    if (cleanerBoxOpenUiState.showConfirmationDialog) {
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
                                cleanerBoxOpenViewModel.confirmBoxOpening(false)
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
                                cleanerBoxOpenViewModel.confirmBoxOpening(true)
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
        // Clean header with solid Airbnb red
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
                    .padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    visible = isContentVisible,
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(600)
                    ) + fadeIn(animationSpec = tween(600))
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
                                 imageVector = Icons.Default.Build,
                                 contentDescription = null,
                                 tint = Color.White,
                                 modifier = Modifier.size(32.dp)
                             )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Cleaner Dashboard",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                        
                        if (name != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Hello, $name! 🧹",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content area
        AnimatedVisibility(
            visible = isContentVisible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(800, delayMillis = 200)
            ) + fadeIn(animationSpec = tween(800, delayMillis = 200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when {
                    isLoading || cleanerBoxOpenUiState.isLoading -> {
                        // Loading state
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = airbnbRed,
                                modifier = Modifier.size(48.dp),
                                strokeWidth = 4.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                if (isLoading) "Loading orders..." else "Opening box...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = textDark
                            )
                        }
                    }

                    !errorMessage.isNullOrEmpty() || !cleanerBoxOpenUiState.errorMessage.isNullOrEmpty() -> {
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
                                text = "Error Loading Orders",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = textDark
                            )
                            Text(
                                text = errorMessage ?: cleanerBoxOpenUiState.errorMessage ?: "Unknown error",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textLight,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        isLoading = true
                                        errorMessage = null
                                        cleanerBoxOpenViewModel.clearMessages()
                                        try {
                                            cleanerRepository.getAllExtraOrders()
                                                .onSuccess { orders ->
                                                    extraOrders = orders
                                                    errorMessage = null
                                                }
                                                .onFailure { exception ->
                                                    errorMessage = exception.message ?: "Failed to load orders"
                                                }
                                        } catch (e: Exception) {
                                            errorMessage = e.message
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = airbnbRed)
                            ) {
                                Text("Retry")
                            }
                        }
                    }

                    extraOrders.isEmpty() -> {
                        // Empty state
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = successGreen,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "All Caught Up!",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = textDark
                            )
                            Text(
                                text = "No pending orders need fulfillment at this time.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textLight,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    else -> {
                        // Orders list
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(extraOrders) { order ->
                                ExtraOrderCard(
                                    order = order,
                                    onFulfillClick = { handleFulfillOrder(order) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Notes Dialog for fulfillment
    if (showNotesDialog && selectedOrder != null) {
        AlertDialog(
            onDismissRequest = {
                showNotesDialog = false
                selectedOrder = null
                fulfillNotes = ""
                cleanerBoxOpenViewModel.resetState()
            },
            title = {
                Text(
                    text = "Add Delivery Notes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Order #${selectedOrder!!.id}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textLight
                    )
                    Text(
                        text = "Box opened successfully! ✅",
                        style = MaterialTheme.typography.bodyMedium,
                        color = successGreen,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = fulfillNotes,
                        onValueChange = { fulfillNotes = it },
                        label = { Text("Delivery Notes") },
                        placeholder = { Text("e.g., Delivered toilet paper to room 101. Customer was satisfied.") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = airbnbRed,
                            focusedLabelColor = airbnbRed
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { submitFulfillment() },
                    colors = ButtonDefaults.buttonColors(containerColor = airbnbRed),
                    enabled = !cleanerBoxOpenUiState.isLoading
                ) {
                    if (cleanerBoxOpenUiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Complete Fulfillment")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNotesDialog = false
                        selectedOrder = null
                        fulfillNotes = ""
                        cleanerBoxOpenViewModel.resetState()
                    },
                    enabled = !cleanerBoxOpenUiState.isLoading
                ) {
                    Text("Cancel", color = textLight)
                }
            }
        )
    }
}

@Composable
fun ExtraOrderCard(
    order: ExtraOrder,
    onFulfillClick: () -> Unit
) {
    // Airbnb-style color palette
    val airbnbRed = Color(0xFFFF5A5F)
    val cardWhite = Color(0xFFFFFFFF)
    val textDark = Color(0xFF484848)
    val textLight = Color(0xFF767676)
    val successGreen = Color(0xFF00C851)
    val orangeWarning = Color(0xFFFF9800)

    val statusColor = when (order.status.uppercase()) {
        "DELIVERED", "FULFILLED" -> successGreen
        "CONFIRMED" -> orangeWarning
        "PENDING" -> airbnbRed
        else -> textLight
    }

    val isPending = order.status.uppercase() == "PENDING"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = cardWhite),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header row with reservation info and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Reservation #${order.reservation.id}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textDark
                    )
                    Text(
                        text = "Order #${order.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = textLight
                    )
                    Text(
                        text = "Guest: ${order.reservation.guest.name} ${order.reservation.guest.surname}",
                        style = MaterialTheme.typography.bodySmall,
                        color = textLight
                    )
                }
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = statusColor.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = order.status.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

                        // Order details - show all items
            Column(modifier = Modifier.fillMaxWidth()) {
                // Items list
                order.items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.inventoryItem.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = textDark,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            item.inventoryItem.description?.let { description ->
                                if (description.isNotBlank()) {
                                    Text(
                                        text = description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = textLight,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Text(
                                text = "Quantity: ${item.quantity} × €${item.unitPrice}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textLight
                            )
                        }
                        
                        Text(
                            text = "€${item.totalPrice}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = airbnbRed
                        )
                    }
                    
                    if (item != order.items.last()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Total and FULFILL button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total: €${order.totalPrice}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textDark
                    )
                    
                    // FULFILL button - only show for pending orders
                    if (isPending) {
                        Button(
                            onClick = onFulfillClick,
                            colors = ButtonDefaults.buttonColors(containerColor = airbnbRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "FULFILL",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Notes if present
            order.notes?.let { notes ->
                if (notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF5F5F5)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Note: $notes",
                            style = MaterialTheme.typography.bodySmall,
                            color = textLight,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Timestamps
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Ordered: ${order.createdAt}",
                style = MaterialTheme.typography.bodySmall,
                color = textLight
            )
            order.fulfilledAt?.let { fulfilledAt ->
                Text(
                    text = "Fulfilled: $fulfilledAt",
                    style = MaterialTheme.typography.bodySmall,
                    color = successGreen
                )
            }
        }
    }
}