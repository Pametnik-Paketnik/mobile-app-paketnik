package com.jvn.myapplication.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jvn.myapplication.data.model.BoxData
import com.jvn.myapplication.data.repository.AuthRepository
import com.jvn.myapplication.data.repository.BoxRepository
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxesListScreen(
    onBoxClick: (BoxData) -> Unit = {}
) {
    // Airbnb-style color palette
    val airbnbRed = Color(0xFFFF5A5F)
    val darkGray = Color(0xFF484848)
    val lightGray = Color(0xFFF7F7F7)
    val cardWhite = Color(0xFFFFFFFF)
    val textDark = Color(0xFF484848)
    val textLight = Color(0xFF767676)
    val successGreen = Color(0xFF00A699)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Repositories
    val authRepository = remember { AuthRepository(context) }
    val boxRepository = remember { BoxRepository(context) }

    // State variables
    var boxes by remember { mutableStateOf<List<BoxData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isContentVisible by remember { mutableStateOf(false) }

    // User data from repository
    val username by authRepository.getUsername().collectAsState(initial = null)

    // Load boxes on screen start
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val result = boxRepository.getAllBoxes()
                result.fold(
                    onSuccess = { boxList ->
                        boxes = boxList
                        errorMessage = null
                        delay(300)
                        isContentVisible = true
                    },
                    onFailure = { exception ->
                        errorMessage = exception.message ?: "Failed to load boxes"
                        isContentVisible = true
                    }
                )
            } catch (e: Exception) {
                errorMessage = e.message ?: "An error occurred"
                isContentVisible = true
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightGray)
    ) {
        // Header with Airbnb red background
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
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Available Boxes",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                        
                        if (!username.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Find your perfect box, $username! 🏠",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Content area
        AnimatedVisibility(
            visible = isContentVisible,
            enter = scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(animationSpec = tween(1000))
        ) {
            when {
                isLoading -> {
                    // Loading state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = airbnbRed)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Loading available boxes...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = textLight
                            )
                        }
                    }
                }
                
                errorMessage != null -> {
                    // Error state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = airbnbRed,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Oops! Something went wrong",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = textDark,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                errorMessage ?: "Unknown error occurred",
                                style = MaterialTheme.typography.bodyLarge,
                                color = textLight,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    // Retry loading
                                    scope.launch {
                                        isLoading = true
                                        errorMessage = null
                                        try {
                                            val result = boxRepository.getAllBoxes()
                                            result.fold(
                                                onSuccess = { boxList ->
                                                    boxes = boxList
                                                    errorMessage = null
                                                },
                                                onFailure = { exception ->
                                                    errorMessage = exception.message ?: "Failed to load boxes"
                                                }
                                            )
                                        } catch (e: Exception) {
                                            errorMessage = e.message ?: "An error occurred"
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = airbnbRed),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Try Again")
                            }
                        }
                    }
                }
                
                boxes.isEmpty() -> {
                    // Empty state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = textLight,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No boxes available",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = textDark,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Check back later for new listings!",
                                style = MaterialTheme.typography.bodyLarge,
                                color = textLight,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                else -> {
                    // Boxes list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            // Summary card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = cardWhite),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = successGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "${boxes.size} boxes available",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textDark
                                    )
                                }
                            }
                        }
                        
                        items(boxes) { box ->
                            BoxCard(
                                box = box,
                                onClick = { onBoxClick(box) }
                            )
                        }
                        
                        // Bottom padding for navigation bar
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BoxCard(
    box: BoxData,
    onClick: () -> Unit
) {
    // Airbnb-style colors
    val airbnbRed = Color(0xFFFF5A5F)
    val darkGray = Color(0xFF484848)
    val lightGray = Color(0xFFF7F7F7)
    val cardWhite = Color(0xFFFFFFFF)
    val textDark = Color(0xFF484848)
    val textLight = Color(0xFF767676)
    val successGreen = Color(0xFF00A699)
    
    val context = LocalContext.current
    val boxRepository = remember { BoxRepository(context) }
    val scope = rememberCoroutineScope()
    
    // State for availability checking
    var isCurrentlyUnavailable by remember { mutableStateOf(false) }
    var isCheckingAvailability by remember { mutableStateOf(false) }
    
    // Check if box is currently unavailable (today's date falls within unavailable ranges)
    LaunchedEffect(box.boxId) {
        if (box.boxId != null) {
            isCheckingAvailability = true
            scope.launch {
                boxRepository.getBoxAvailability(box.boxId).fold(
                    onSuccess = { availability ->
                        val today = LocalDate.now()
                        
                        // Check if today falls within any active unavailable date range
                        isCurrentlyUnavailable = availability.unavailableDates.any { dateRange ->
                            try {
                                // Only consider dates unavailable if the reservation is active
                                val isActiveReservation = when (dateRange.status.uppercase()) {
                                    "PENDING", "CHECKED_IN", "BOOKED", "CONFIRMED" -> true
                                    "CHECKED_OUT", "COMPLETED", "CANCELLED" -> false
                                    else -> true // Default to unavailable for unknown statuses
                                }
                                
                                if (isActiveReservation) {
                                    val startDate = LocalDate.parse(dateRange.startDate.take(10))
                                    val endDate = LocalDate.parse(dateRange.endDate.take(10))
                                    
                                    // Check if today is within this range (inclusive)
                                    !today.isBefore(startDate) && !today.isAfter(endDate)
                                } else {
                                    false // Inactive reservations don't make dates unavailable
                                }
                            } catch (e: Exception) {
                                false // If parsing fails, assume available
                            }
                        }
                    },
                    onFailure = {
                        // If API fails, assume available
                        isCurrentlyUnavailable = false
                    }
                )
                isCheckingAvailability = false
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.1f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Using custom shadow
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Box image - use actual image from API or fallback to placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                contentAlignment = Alignment.Center
            ) {
                val primaryImage = box.images?.find { it.isPrimary == true } ?: box.images?.firstOrNull()
                
                if (primaryImage?.imageUrl != null) {
                    // Transform image URL based on current development mode (emulator vs real device)
                    val imageUrl = com.jvn.myapplication.config.ApiConfig.transformImageUrl(primaryImage.imageUrl)
                    
                    if (imageUrl != null) {
                        // State to track image loading
                        var imageState by remember { mutableStateOf<String>("loading") }
                        
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Always render AsyncImage so callbacks can fire
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imageUrl)
                                    .crossfade(true)
                                    .listener(
                                        onStart = { imageState = "loading" },
                                        onSuccess = { _, _ -> imageState = "success" },
                                        onError = { _, _ -> imageState = "error" }
                                    )
                                    .build(),
                                contentDescription = "Box image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        
                        // Overlay loading/error states on top
                        when (imageState) {
                            "loading" -> {
                                // Loading overlay
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(lightGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFFFF5A5F),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            "error" -> {
                                // Error overlay
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                                colors = listOf(lightGray, lightGray.copy(alpha = 0.8f))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Home,
                                            contentDescription = null,
                                            tint = textLight,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Image not available",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = textLight
                                        )
                                    }
                                }
                            }
                            // "success" state shows the image without overlay
                        }
                    }
                    } else {
                        // URL transformation failed - show placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(lightGray, lightGray.copy(alpha = 0.8f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = null,
                                    tint = textLight,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Image not available",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textLight
                                )
                            }
                        }
                    }
                } else {
                    // No images available - show placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(lightGray, lightGray.copy(alpha = 0.8f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                tint = textLight,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Box ${box.boxId ?: "Unknown"}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = textDark
                            )
                        }
                    }
                }
            }
            
            // Box details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Title and Box ID
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = box.location ?: "Unknown Location",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textDark,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Box ID: ${box.boxId ?: "Unknown"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textLight
                        )
                    }
                    
                    // Status badge - dynamic based on current availability
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = when {
                            isCheckingAvailability -> Color.Gray
                            isCurrentlyUnavailable -> airbnbRed
                            else -> successGreen
                        },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = when {
                                isCheckingAvailability -> "CHECKING..."
                                isCurrentlyUnavailable -> "CURRENTLY UNAVAILABLE"
                                else -> "AVAILABLE"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Price and View button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${box.pricePerNight ?: "0"} € / night",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textDark
                        )
                        Text(
                            text = if (box.owner?.username != null) "Hosted by ${box.owner.username}" else "Available now",
                            style = MaterialTheme.typography.bodySmall,
                            color = textLight
                        )
                    }
                    
                    // View button
                    Button(
                        onClick = onClick,
                        colors = ButtonDefaults.buttonColors(containerColor = airbnbRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(
                            text = "View",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
} 