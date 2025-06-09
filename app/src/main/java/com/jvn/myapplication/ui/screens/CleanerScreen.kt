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
import com.jvn.myapplication.data.model.ExtraOrder
import com.jvn.myapplication.data.repository.AuthRepository
import com.jvn.myapplication.data.repository.CleanerRepository
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

    // State variables
    var extraOrders by remember { mutableStateOf<List<ExtraOrder>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isContentVisible by remember { mutableStateOf(false) }
    var selectedOrderId by remember { mutableStateOf<Int?>(null) }
    var isScanningActive by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }
    var fulfillNotes by remember { mutableStateOf("") }

    // User data from repository
    val name by authRepository.getName().collectAsState(initial = null)
    val userType by authRepository.getUserType().collectAsState(initial = null)

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
    fun handleFulfillOrder(orderId: Int) {
        selectedOrderId = orderId
        fulfillNotes = ""
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            isScanningActive = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Handle notes submission after QR scan
    fun submitFulfillment() {
        val orderId = selectedOrderId
        val notes = fulfillNotes.trim().takeIf { it.isNotBlank() }
        
        if (orderId != null) {
            scope.launch {
                try {
                    cleanerRepository.fulfillOrder(orderId, notes)
                        .onSuccess {
                            Toast.makeText(context, "✅ Order fulfilled successfully!", Toast.LENGTH_LONG).show()
                            // Reload orders to update the status
                            cleanerRepository.getAllExtraOrders()
                                .onSuccess { orders ->
                                    extraOrders = orders
                                }
                        }
                        .onFailure { exception ->
                            Toast.makeText(context, "❌ Failed to fulfill order: ${exception.message}", Toast.LENGTH_LONG).show()
                        }
                } catch (e: Exception) {
                    Toast.makeText(context, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
                
                // Reset state
                selectedOrderId = null
                showNotesDialog = false
                fulfillNotes = ""
            }
        }
    }

    if (isScanningActive && selectedOrderId != null) {
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
                        onQrCodeScanned = { boxId ->
                            isScanningActive = false
                            Toast.makeText(context, "✅ Box opened! Now add delivery notes.", Toast.LENGTH_SHORT).show()
                            showNotesDialog = true
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { 
                        isScanningActive = false
                        selectedOrderId = null
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
                    isLoading -> {
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
                                "Loading orders...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = textDark
                            )
                        }
                    }

                    !errorMessage.isNullOrEmpty() -> {
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
                                text = errorMessage!!,
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
                                    onFulfillClick = { handleFulfillOrder(order.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Notes Dialog for fulfillment
    if (showNotesDialog && selectedOrderId != null) {
        AlertDialog(
            onDismissRequest = {
                showNotesDialog = false
                selectedOrderId = null
                fulfillNotes = ""
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
                        text = "Order #$selectedOrderId",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textLight
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
                    colors = ButtonDefaults.buttonColors(containerColor = airbnbRed)
                ) {
                    Text("Complete Fulfillment")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNotesDialog = false
                        selectedOrderId = null
                        fulfillNotes = ""
                    }
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