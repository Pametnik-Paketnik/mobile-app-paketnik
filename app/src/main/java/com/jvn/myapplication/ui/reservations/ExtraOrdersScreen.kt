package com.jvn.myapplication.ui.reservations

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jvn.myapplication.data.model.InventoryItem
import com.jvn.myapplication.data.model.Reservation
import com.jvn.myapplication.data.repository.AuthRepository
import com.jvn.myapplication.data.repository.ExtraOrderRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtraOrdersScreen(
    reservation: Reservation,
    onBackClick: () -> Unit
) {
    // Airbnb-style color palette
    val airbnbRed = Color(0xFFFF5A5F)
    val lightGray = Color(0xFFF7F7F7)
    val cardWhite = Color(0xFFFFFFFF)
    val textDark = Color(0xFF484848)
    val textLight = Color(0xFF767676)

    val context = LocalContext.current
    val extraOrderRepository = remember { ExtraOrderRepository(context) }
    val scope = rememberCoroutineScope()

    // State for animations
    var isContentVisible by remember { mutableStateOf(false) }
    var inventoryItems by remember { mutableStateOf<List<InventoryItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isCreatingOrder by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Track quantities for each item
    var itemQuantities by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var notes by remember { mutableStateOf("") }

    LaunchedEffect(reservation.id) {
        isLoading = true
        errorMessage = null
        successMessage = null
        
        try {
            val hostId = reservation.host?.id
            if (hostId != null) {
                println("🔍 DEBUG - ExtraOrdersScreen: Loading inventory for reservation ${reservation.id}, host $hostId")
                
                extraOrderRepository.getInventoryForReservation(reservation.id, hostId)
                    .onSuccess { loadedInventoryItems ->
                        println("🔍 DEBUG - ExtraOrdersScreen: Loaded ${loadedInventoryItems.size} inventory items")
                        inventoryItems = loadedInventoryItems
                        
                        // Initialize quantities to 0 for all items
                        itemQuantities = loadedInventoryItems.associate { it.id to 0 }
                        
                        errorMessage = null
                    }
                    .onFailure { exception ->
                        println("🔍 DEBUG - ExtraOrdersScreen: Error loading inventory: ${exception.message}")
                        errorMessage = exception.message ?: "Failed to load inventory"
                        inventoryItems = emptyList()
                    }
            } else {
                errorMessage = "No host information found for this reservation"
            }
        } catch (e: Exception) {
            println("🔍 DEBUG - ExtraOrdersScreen: Exception: ${e.message}")
            errorMessage = e.message
            inventoryItems = emptyList()
        } finally {
            isLoading = false
        }
        
        delay(200)
        isContentVisible = true
    }

    // Function to handle quantity changes
    fun updateQuantity(itemId: Int, newQuantity: Int, maxQuantity: Int) {
        val safeQuantity = newQuantity.coerceIn(0, maxQuantity)
        itemQuantities = itemQuantities.toMutableMap().apply {
            put(itemId, safeQuantity)
        }
    }

    // Function to create batch order
    suspend fun createBatchOrder() {
        val itemsToOrder = itemQuantities.filter { it.value > 0 }
            .map { (itemId, quantity) -> itemId to quantity }
        
        if (itemsToOrder.isEmpty()) {
            errorMessage = "Please select at least one item to order"
            return
        }
        
        isCreatingOrder = true
        try {
            extraOrderRepository.createExtraOrder(
                reservationId = reservation.id,
                items = itemsToOrder,
                notes = notes.takeIf { it.isNotBlank() }
            ).onSuccess { response ->
                if (response.success) {
                    successMessage = "Order has been requested successfully!"
                    // Reset all quantities
                    itemQuantities = itemQuantities.mapValues { 0 }
                    notes = ""
                } else {
                    errorMessage = response.message
                }
            }.onFailure { exception ->
                errorMessage = exception.message ?: "Failed to create order"
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: "An error occurred"
        } finally {
            isCreatingOrder = false
        }
    }

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
                    .padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)
            ) {
                AnimatedVisibility(
                    visible = isContentVisible,
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(600)
                    ) + fadeIn(animationSpec = tween(600))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Column {
                            Text(
                                text = "Available Items",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Reservation #${reservation.id} • ${reservation.box?.boxId ?: "Unknown Box"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }

        // Success message
        successMessage?.let { message ->
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = message,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { successMessage = null }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Error message
        errorMessage?.let { message ->
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = airbnbRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = message,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { errorMessage = null }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

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
                                "Loading available items...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = textDark
                            )
                        }
                    }

                    inventoryItems.isEmpty() -> {
                        // Empty state
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = textLight,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Items Available",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = textDark
                            )
                            Text(
                                text = "Your host doesn't have any items available for ordering right now.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textLight,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    else -> {
                        // Inventory items list with notes and order button
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(inventoryItems) { item ->
                                InventoryItemCard(
                                    item = item,
                                    quantity = itemQuantities[item.id] ?: 0,
                                    onQuantityChange = { newQuantity ->
                                        updateQuantity(item.id, newQuantity, item.availableQuantity)
                                    }
                                )
                            }
                            
                            // Notes input field
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(6.dp, RoundedCornerShape(20.dp)),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp)
                                    ) {
                                        Text(
                                            text = "Order Notes",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = textDark
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = notes,
                                            onValueChange = { notes = it },
                                            placeholder = { 
                                                Text(
                                                    text = "Add any special instructions (optional)",
                                                    color = textLight
                                                ) 
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = airbnbRed,
                                                cursorColor = airbnbRed
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            maxLines = 3
                                        )
                                    }
                                }
                            }
                            
                            // Order all button
                            item {
                                val totalItems = itemQuantities.values.sum()
                                val hasItems = totalItems > 0
                                
                                Button(
                                    onClick = { 
                                        scope.launch {
                                            createBatchOrder()
                                        }
                                    },
                                    enabled = hasItems && !isCreatingOrder,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = airbnbRed,
                                        disabledContainerColor = Color(0xFFE0E0E0)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .shadow(6.dp, RoundedCornerShape(16.dp))
                                ) {
                                    if (isCreatingOrder) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ShoppingCart,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (totalItems > 0) "ORDER ALL ITEMS ($totalItems)" else "SELECT ITEMS TO ORDER",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
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
    }
}

@Composable
fun InventoryItemCard(
    item: InventoryItem,
    quantity: Int,
    onQuantityChange: (Int) -> Unit
) {
    // Airbnb-style color palette
    val airbnbRed = Color(0xFFFF5A5F)
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
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Item image
                Card(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    if (!item.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(item.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = item.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = textLight,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Item details
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textDark,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    item.description?.let { description ->
                        if (description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = textLight,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "€${item.price}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = airbnbRed
                    )

                    Text(
                        text = "${item.availableQuantity} available",
                        style = MaterialTheme.typography.bodySmall,
                        color = textLight
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quantity selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Minus button
                IconButton(
                    onClick = { 
                        if (quantity > 0) {
                            onQuantityChange(quantity - 1)
                        }
                    },
                    enabled = quantity > 0,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (quantity > 0) airbnbRed.copy(alpha = 0.1f) 
                            else Color(0xFFF5F5F5)
                        )
                ) {
                    Text(
                        text = "−",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (quantity > 0) airbnbRed else textLight,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Quantity display
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .width(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = quantity.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textDark
                    )
                }

                // Plus button
                IconButton(
                    onClick = { 
                        if (quantity < item.availableQuantity) {
                            onQuantityChange(quantity + 1)
                        }
                    },
                    enabled = quantity < item.availableQuantity,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (quantity < item.availableQuantity) airbnbRed.copy(alpha = 0.1f) 
                            else Color(0xFFF5F5F5)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase",
                        tint = if (quantity < item.availableQuantity) airbnbRed else textLight,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
} 