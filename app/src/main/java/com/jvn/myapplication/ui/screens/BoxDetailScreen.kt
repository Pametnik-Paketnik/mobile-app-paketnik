package com.jvn.myapplication.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jvn.myapplication.data.model.BoxData
import com.jvn.myapplication.data.repository.BoxRepository
import com.jvn.myapplication.data.repository.AuthRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxDetailScreen(
    box: BoxData,
    onBackClick: () -> Unit
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
    val scope = rememberCoroutineScope()
    
    // Repositories
    val boxRepository = remember { BoxRepository(context) }
    val authRepository = remember { AuthRepository(context) }
    
    // State for date range and availability
    var selectedDateRange by remember { mutableStateOf<Pair<LocalDate?, LocalDate?>>(null to null) }
    var unavailableDates by remember { mutableStateOf<Set<LocalDate>>(emptySet()) }
    var showDateRangePicker by remember { mutableStateOf(false) }
    var isLoadingAvailability by remember { mutableStateOf(false) }
    
    // Get current user ID for booking
    val currentUserId by authRepository.getUserId().collectAsState(initial = null)

    // Calculate total price
    val totalPrice = remember(selectedDateRange) {
        val (startDate, endDate) = selectedDateRange
        if (startDate != null && endDate != null) {
            val nights = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt()
            if (nights > 0) {
                val pricePerNight = box.pricePerNight?.toDoubleOrNull() ?: 0.0
                nights * pricePerNight
            } else 0.0
        } else 0.0
    }

    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    
    // Load availability when screen opens
    LaunchedEffect(box.boxId) {
        if (box.boxId != null) {
            isLoadingAvailability = true
            scope.launch {
                boxRepository.getBoxAvailability(box.boxId).fold(
                    onSuccess = { availability ->
                        // Parse unavailable dates from ISO strings to LocalDate
                        unavailableDates = availability.unavailableDates.mapNotNull { dateString ->
                            try {
                                LocalDate.parse(dateString.take(10)) // Take only date part from ISO string
                            } catch (e: Exception) {
                                null
                            }
                        }.toSet()
                    },
                    onFailure = {
                        // Handle error - for now just continue with empty unavailable dates
                        unavailableDates = emptySet()
                    }
                )
                isLoadingAvailability = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightGray)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                // Image Gallery
                ImageGallery(
                    box = box,
                    onBackClick = onBackClick
                )
            }
            
            item {
                // Box Details
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Title and host info
                        Text(
                            text = box.location ?: "Unknown Location",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = textDark
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = if (box.owner?.username != null) "Hosted by ${box.owner.username}" else "Available now",
                            style = MaterialTheme.typography.bodyLarge,
                            color = textLight
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Box ID and Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Box ID: ${box.boxId ?: "Unknown"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textLight
                            )
                            
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = successGreen
                            ) {
                                Text(
                                    text = "AVAILABLE",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Description
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textDark
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Lepo opremljen box v središču mesta. Odličen za shranjevanje vaših stvari med potovanjem. Varen, suh in dostopen 24/7.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textLight,
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
                        )
                    }
                }
            }
            
            item {
                // Price Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "${box.pricePerNight ?: "0"}€",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = textDark
                            )
                            Text(
                                text = " / night",
                                style = MaterialTheme.typography.bodyLarge,
                                color = textLight
                            )
                        }
                    }
                }
            }
            
            item {
                // Date Selection Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Select Dates",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textDark
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Date Range Selection
                        OutlinedTextField(
                            value = if (selectedDateRange.first != null && selectedDateRange.second != null) {
                                "${selectedDateRange.first?.format(dateFormatter)} - ${selectedDateRange.second?.format(dateFormatter)}"
                            } else if (selectedDateRange.first != null) {
                                "${selectedDateRange.first?.format(dateFormatter)} - Select end date"
                            } else {
                                ""
                            },
                            onValueChange = { },
                            label = { Text("Select dates") },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { showDateRangePicker = true }) {
                                    Icon(Icons.Default.DateRange, contentDescription = "Select date range")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Tap to select check-in and check-out dates") }
                        )
                        
                        // Show availability loading
                        if (isLoadingAvailability) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = airbnbRed
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Loading availability...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textLight
                                )
                            }
                        }
                        
                        // Availability legend
                        if (unavailableDates.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(Color.Red, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Unavailable dates",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textLight
                                )
                            }
                        }
                        
                        // Total price calculation
                        if (totalPrice > 0) {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Divider()
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    val nights = if (selectedDateRange.first != null && selectedDateRange.second != null) {
                                        java.time.temporal.ChronoUnit.DAYS.between(selectedDateRange.first!!, selectedDateRange.second!!).toInt()
                                    } else 0
                                    
                                    Text(
                                        text = "Total for $nights nights",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = textDark
                                    )
                                    Text(
                                        text = "Including taxes and fees",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = textLight
                                    )
                                }
                                
                                Text(
                                    text = "€${String.format("%.2f", totalPrice)}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = airbnbRed
                                )
                            }
                        }
                    }
                }
            }
            
            item {
                // Book Button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Button(
                        onClick = {
                            // TODO: Implement booking functionality with API call
                            if (selectedDateRange.first != null && selectedDateRange.second != null && 
                                currentUserId != null && box.owner?.id != null && box.boxId != null) {
                                scope.launch {
                                    // Create reservation API call would go here
                                    // val request = CreateReservationRequest(...)
                                    // reservationRepository.createReservation(request)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = airbnbRed),
                        shape = RoundedCornerShape(12.dp),
                        enabled = selectedDateRange.first != null && selectedDateRange.second != null && totalPrice > 0
                    ) {
                        Text(
                            text = if (totalPrice > 0) "Book for €${String.format("%.2f", totalPrice)}" else "Select dates to book",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
    
    // Date range picker
    if (showDateRangePicker) {
        DateRangePickerDialog(
            onDateRangeSelected = { startDate, endDate ->
                selectedDateRange = startDate to endDate
                showDateRangePicker = false
            },
            onDismiss = { showDateRangePicker = false },
            unavailableDates = unavailableDates,
            initialStartDate = selectedDateRange.first,
            initialEndDate = selectedDateRange.second
        )
    }
}

@Composable
fun ImageGallery(
    box: BoxData,
    onBackClick: () -> Unit
) {
    val lightGray = Color(0xFFF7F7F7)
    val textLight = Color(0xFF767676)
    
    // Get all images or create empty list
    val images = box.images?.filter { !it.imageUrl.isNullOrEmpty() } ?: emptyList()
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        if (images.isNotEmpty()) {
            val pagerState = rememberPagerState(pageCount = { images.size })
            
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val image = images[page]
                val imageUrl = image.imageUrl?.let { url ->
                    var finalUrl = url
                        .replace("localhost", "10.0.2.2")
                        .replace("127.0.0.1", "10.0.2.2")
                    
                    if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
                        finalUrl = "http://$finalUrl"
                    }
                    finalUrl
                }
                
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Box image ${page + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            // Page indicators
            if (images.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(images.size) { index ->
                        Box(
                            modifier = Modifier
                                .size(if (pagerState.currentPage == index) 8.dp else 6.dp)
                                .background(
                                    color = if (pagerState.currentPage == index) Color.White else Color.White.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(50)
                                )
                        )
                    }
                }
            }
        } else {
            // No images placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(lightGray),
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
                        text = "No images available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textLight
                    )
                }
            }
        }
        
        // Back button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(
                    Color.Black.copy(alpha = 0.5f),
                    RoundedCornerShape(50)
                )
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    onDateRangeSelected: (LocalDate?, LocalDate?) -> Unit,
    onDismiss: () -> Unit,
    unavailableDates: Set<LocalDate>,
    initialStartDate: LocalDate? = null,
    initialEndDate: LocalDate? = null
) {
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartDate?.let { it.toEpochDay() * 24 * 60 * 60 * 1000L },
        initialSelectedEndDateMillis = initialEndDate?.let { it.toEpochDay() * 24 * 60 * 60 * 1000L }
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val startMillis = dateRangePickerState.selectedStartDateMillis
                    val endMillis = dateRangePickerState.selectedEndDateMillis
                    
                    val startDate = startMillis?.let { 
                        LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000L))
                    }
                    val endDate = endMillis?.let { 
                        LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000L))
                    }
                    
                    // Always allow confirmation if dates are selected, let user decide about unavailable dates
                    if (startDate != null || endDate != null) {
                        onDateRangeSelected(startDate, endDate)
                    }
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                DateRangePicker(
                    state = dateRangePickerState,
                    modifier = Modifier.height(400.dp) // Fixed height instead of weight
                )
                
                // Show warning if unavailable dates exist in selection
                val startMillis = dateRangePickerState.selectedStartDateMillis
                val endMillis = dateRangePickerState.selectedEndDateMillis
                
                if (startMillis != null && endMillis != null) {
                    val startDate = LocalDate.ofEpochDay(startMillis / (24 * 60 * 60 * 1000L))
                    val endDate = LocalDate.ofEpochDay(endMillis / (24 * 60 * 60 * 1000L))
                    
                    var current: LocalDate = startDate
                    var hasUnavailableDate = false
                    while (!current.isAfter(endDate)) {
                        if (unavailableDates.contains(current)) {
                            hasUnavailableDate = true
                            break
                        }
                        current = current.plusDays(1)
                    }
                    
                    if (hasUnavailableDate) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f))
                        ) {
                            Text(
                                text = "⚠️ Warning: Selected range contains unavailable dates. Booking may not be possible for those dates.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Red,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        },
        title = {
            Text("Select Date Range")
        }
    )
} 