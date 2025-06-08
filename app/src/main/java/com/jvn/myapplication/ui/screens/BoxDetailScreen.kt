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
import com.jvn.myapplication.data.repository.ReservationRepository
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
    val reservationRepository = remember { ReservationRepository(context) }
    
    // State for date range and availability
    var selectedDateRange by remember { mutableStateOf<Pair<LocalDate?, LocalDate?>>(null to null) }
    var unavailableDates by remember { mutableStateOf<Set<LocalDate>>(emptySet()) }
    var showDateRangePicker by remember { mutableStateOf(false) }
    var isLoadingAvailability by remember { mutableStateOf(false) }
    
    // State for booking
    var isBooking by remember { mutableStateOf(false) }
    var bookingError by remember { mutableStateOf<String?>(null) }
    var bookingSuccess by remember { mutableStateOf(false) }
    
    // State for current availability status
    var isCurrentlyUnavailable by remember { mutableStateOf(false) }
    var isCheckingCurrentAvailability by remember { mutableStateOf(false) }
    
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
            isCheckingCurrentAvailability = true
            scope.launch {
                boxRepository.getBoxAvailability(box.boxId).fold(
                    onSuccess = { availability ->
                        // Parse unavailable date ranges and collect all dates within those ranges
                        val allUnavailableDates = mutableSetOf<LocalDate>()
                        val today = LocalDate.now()
                        
                        availability.unavailableDates.forEach { dateRange ->
                            try {
                                // Only consider dates unavailable if the reservation is active
                                // Skip CHECKED_OUT, COMPLETED, CANCELLED reservations
                                val isActiveReservation = when (dateRange.status.uppercase()) {
                                    "PENDING", "CHECKED_IN", "BOOKED", "CONFIRMED" -> true
                                    "CHECKED_OUT", "COMPLETED", "CANCELLED" -> false
                                    else -> true // Default to unavailable for unknown statuses
                                }
                                
                                if (isActiveReservation) {
                                    val startDate = LocalDate.parse(dateRange.startDate.take(10))
                                    val endDate = LocalDate.parse(dateRange.endDate.take(10))
                                    
                                    // Check if today falls within this range for current status
                                    if (!today.isBefore(startDate) && !today.isAfter(endDate)) {
                                        isCurrentlyUnavailable = true
                                    }
                                    
                                    // Add all dates from startDate to endDate (inclusive)
                                    var currentDate = startDate
                                    while (!currentDate.isAfter(endDate)) {
                                        allUnavailableDates.add(currentDate)
                                        currentDate = currentDate.plusDays(1)
                                    }
                                }
                                
                                println("🔍 DEBUG - BoxDetailScreen: Date range ${dateRange.startDate} to ${dateRange.endDate}, status: ${dateRange.status}, active: $isActiveReservation")
                            } catch (e: Exception) {
                                println("🔍 DEBUG - BoxDetailScreen: Error parsing date range: ${e.message}")
                            }
                        }
                        
                        unavailableDates = allUnavailableDates
                        println("🔍 DEBUG - BoxDetailScreen: Loaded ${unavailableDates.size} unavailable dates")
                    },
                    onFailure = { exception ->
                        println("🔍 DEBUG - BoxDetailScreen: Error loading availability: ${exception.message}")
                        unavailableDates = emptySet()
                        isCurrentlyUnavailable = false
                    }
                )
                isLoadingAvailability = false
                isCheckingCurrentAvailability = false
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
                                color = when {
                                    isCheckingCurrentAvailability -> Color.Gray
                                    isCurrentlyUnavailable -> airbnbRed
                                    else -> successGreen
                                }
                            ) {
                                Text(
                                    text = when {
                                        isCheckingCurrentAvailability -> "CHECKING..."
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
                            style = MaterialTheme.typography.titleSmall,
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
                            label = { 
                                Text(
                                    "Select dates",
                                    style = MaterialTheme.typography.bodySmall
                                ) 
                            },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { showDateRangePicker = true }) {
                                    Icon(Icons.Default.DateRange, contentDescription = "Select date range")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { 
                                Text(
                                    "Tap to select check-in and check-out dates",
                                    style = MaterialTheme.typography.bodySmall
                                ) 
                            },
                            textStyle = MaterialTheme.typography.bodyMedium,
                            singleLine = false,
                            maxLines = 2
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
                            if (selectedDateRange.first != null && selectedDateRange.second != null && 
                                currentUserId != null && box.owner?.id != null && box.boxId != null) {
                                
                                // Clear any previous error/success states
                                bookingError = null
                                bookingSuccess = false
                                isBooking = true
                                
                                scope.launch {
                                    try {
                                        // Convert LocalDate to ISO string format for API
                                        val checkinAt = selectedDateRange.first!!.atStartOfDay().toString() + "Z"
                                        val checkoutAt = selectedDateRange.second!!.atStartOfDay().toString() + "Z"
                                        
                                        // Convert currentUserId to int
                                        val guestId = currentUserId!!.toIntOrNull()
                                        if (guestId == null) {
                                            bookingError = "Invalid user ID"
                                            isBooking = false
                                            return@launch
                                        }
                                        
                                        println("🔍 DEBUG - BoxDetailScreen: Starting booking...")
                                        println("🔍 DEBUG - guestId: $guestId, hostId: ${box.owner.id}, boxId: ${box.boxId}")
                                        println("🔍 DEBUG - checkinAt: $checkinAt, checkoutAt: $checkoutAt")
                                        
                                        reservationRepository.createReservation(
                                            guestId = guestId,
                                            hostId = box.owner.id,
                                            boxId = box.boxId,
                                            checkinAt = checkinAt,
                                            checkoutAt = checkoutAt
                                        ).fold(
                                            onSuccess = { reservationResponse ->
                                                println("🔍 DEBUG - BoxDetailScreen: Booking successful!")
                                                println("🔍 DEBUG - Reservation ID: ${reservationResponse.id}")
                                                bookingSuccess = true
                                                isBooking = false
                                            },
                                            onFailure = { exception ->
                                                println("🔍 DEBUG - BoxDetailScreen: Booking failed: ${exception.message}")
                                                bookingError = exception.message ?: "Booking failed"
                                                isBooking = false
                                            }
                                        )
                                    } catch (e: Exception) {
                                        println("🔍 DEBUG - BoxDetailScreen: Exception during booking: ${e.message}")
                                        bookingError = e.message ?: "An unexpected error occurred"
                                        isBooking = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = airbnbRed),
                        shape = RoundedCornerShape(12.dp),
                        enabled = selectedDateRange.first != null && selectedDateRange.second != null && totalPrice > 0 && !isBooking
                    ) {
                        if (isBooking) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Booking...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(
                                text = if (totalPrice > 0) "Book for €${String.format("%.2f", totalPrice)}" else "Select dates to book",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            // Success/Error Messages
            if (bookingSuccess) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = successGreen.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = successGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Booking Successful!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = successGreen
                                )
                                Text(
                                    text = "Your reservation has been created. Check your reservations tab for details.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textDark
                                )
                            }
                        }
                    }
                }
            }
            
            if (bookingError != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = airbnbRed.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = airbnbRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Booking Failed",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = airbnbRed
                                )
                                Text(
                                    text = bookingError!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textDark
                                )
                            }
                            IconButton(onClick = { bookingError = null }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = airbnbRed
                                )
                            }
                        }
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
                val imageUrl = com.jvn.myapplication.config.ApiConfig.transformImageUrl(image.imageUrl)
                
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
            val startMillis = dateRangePickerState.selectedStartDateMillis
            val endMillis = dateRangePickerState.selectedEndDateMillis
            
            // Check if selection is valid (not in past and not unavailable)
            val isValidSelection = if (startMillis != null && endMillis != null) {
                val startDate = LocalDate.ofEpochDay(startMillis / (24 * 60 * 60 * 1000L))
                val endDate = LocalDate.ofEpochDay(endMillis / (24 * 60 * 60 * 1000L))
                val today = LocalDate.now()
                
                // Check if dates are not in past and not unavailable
                var current = startDate
                var isValid = true
                while (!current.isAfter(endDate) && isValid) {
                    if (current.isBefore(today) || unavailableDates.contains(current)) {
                        isValid = false
                    }
                    current = current.plusDays(1)
                }
                isValid
            } else false
            
            TextButton(
                onClick = {
                    val startDate = startMillis?.let { 
                        LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000L))
                    }
                    val endDate = endMillis?.let { 
                        LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000L))
                    }
                    
                    // Only confirm if selection is valid
                    if (isValidSelection && (startDate != null || endDate != null)) {
                        onDateRangeSelected(startDate, endDate)
                    }
                },
                enabled = (startMillis != null || endMillis != null) && isValidSelection
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
                    modifier = Modifier.height(400.dp),
                    headline = {
                        // Custom headline without "Start date" and "End date" text
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                )
                

            }
        },
        title = {
            Text("Select Date Range")
        }
    )
} 