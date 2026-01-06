package com.jvn.myapplication.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
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
import com.jvn.myapplication.data.model.Location
import com.jvn.myapplication.utils.AssetReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen() {
    // Airbnb-style color palette
    val airbnbRed = Color(0xFFFF5A5F)
    val darkGray = Color(0xFF484848)
    val lightGray = Color(0xFFF7F7F7)
    val cardWhite = Color(0xFFFFFFFF)
    val textDark = Color(0xFF484848)
    val textLight = Color(0xFF767676)

    val context = LocalContext.current
    val viewModel: MapViewModel = viewModel { MapViewModel(context) }
    val uiState by viewModel.uiState.collectAsState()
    
    // Load locations from assets
    var locations by remember { mutableStateOf<List<Location>>(emptyList()) }
    var selectedLocationIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var isContentVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    
    // GA Parameters
    var populationSize by remember { mutableStateOf(100) }
    var crossoverRate by remember { mutableStateOf(0.8f) }
    var mutationRate by remember { mutableStateOf(0.1f) }
    var optimizationType by remember { mutableStateOf(OptimizationType.DISTANCE) }

    LaunchedEffect(Unit) {
        // Load locations from CSV
        locations = AssetReader.readLocationsFromAssets(context)
        // Select all locations by default
        selectedLocationIds = locations.map { it.id }.toSet()
        isLoading = false
        kotlinx.coroutines.delay(200)
        isContentVisible = true
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
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Map",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Find nearby package boxes",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content area - List of locations with checkboxes
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = airbnbRed)
                }
            }
            
            locations.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = textLight,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No locations found",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = textDark,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            else -> {
                AnimatedVisibility(
                    visible = isContentVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(800, delayMillis = 100)
                    ) + fadeIn(animationSpec = tween(800, delayMillis = 100))
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Selection info
                        item {
                            if (selectedLocationIds.isNotEmpty()) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(
                                            elevation = 4.dp,
                                            shape = RoundedCornerShape(12.dp),
                                            ambientColor = Color.Black.copy(alpha = 0.1f),
                                            spotColor = Color.Black.copy(alpha = 0.1f)
                                        ),
                                    colors = CardDefaults.cardColors(containerColor = airbnbRed.copy(alpha = 0.1f)),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Text(
                                        text = "Selected: ${selectedLocationIds.size} location(s)",
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = airbnbRed
                                    )
                                }
                            }
                        }
                        
                        // GA Parameters Card
                        item {
                            GAParametersCard(
                                populationSize = populationSize,
                                crossoverRate = crossoverRate,
                                mutationRate = mutationRate,
                                onPopulationSizeChange = { populationSize = it },
                                onCrossoverRateChange = { crossoverRate = it },
                                onMutationRateChange = { mutationRate = it }
                            )
                        }
                        
                        // Optimization Type Selection
                        item {
                            OptimizationTypeCard(
                                selectedType = optimizationType,
                                onTypeSelected = { optimizationType = it }
                            )
                        }
                        
                        // Calculate Button
                        item {
                            Button(
                                onClick = {
                                    viewModel.calculateRoute(
                                        selectedLocationIds = selectedLocationIds.toList(),
                                        optimizationType = optimizationType,
                                        populationSize = populationSize,
                                        crossoverRate = crossoverRate.toDouble(),
                                        mutationRate = mutationRate.toDouble()
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isLoading && selectedLocationIds.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = airbnbRed)
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Calculating...")
                                } else {
                                    Text("Calculate Route")
                                }
                            }
                        }
                        
                        // Error Message
                        item {
                            uiState.errorMessage?.let { error ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = error,
                                        modifier = Modifier.padding(16.dp),
                                        color = Color(0xFFC62828),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                        
                        // Result Display
                        item {
                            uiState.result?.let { result ->
                                RouteResultCard(result = result)
                            }
                        }
                        
                        // Locations list header
                        item {
                            Text(
                                text = "Select Locations:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = textDark,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        // Locations list
                        items(locations) { location ->
                            LocationCard(
                                location = location,
                                isSelected = selectedLocationIds.contains(location.id),
                                onSelectionChange = { isSelected ->
                                    selectedLocationIds = if (isSelected) {
                                        selectedLocationIds + location.id
                                    } else {
                                        selectedLocationIds - location.id
                                    }
                                }
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
private fun LocationCard(
    location: Location,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit
) {
    val airbnbRed = Color(0xFFFF5A5F)
    val cardWhite = Color(0xFFFFFFFF)
    val textDark = Color(0xFF484848)
    val textLight = Color(0xFF767676)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.1f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) airbnbRed.copy(alpha = 0.05f) else cardWhite
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = { onSelectionChange(!isSelected) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectionChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = airbnbRed,
                    uncheckedColor = textLight
                )
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = location.address,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = textDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ID: ${location.id} | Lat: ${String.format("%.6f", location.latitude)}, Lon: ${String.format("%.6f", location.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = textLight
                )
            }
        }
    }
}

@Composable
private fun GAParametersCard(
    populationSize: Int,
    crossoverRate: Float,
    mutationRate: Float,
    onPopulationSizeChange: (Int) -> Unit,
    onCrossoverRateChange: (Float) -> Unit,
    onMutationRateChange: (Float) -> Unit
) {
    val airbnbRed = Color(0xFFFF5A5F)
    val cardWhite = Color(0xFFFFFFFF)
    val textDark = Color(0xFF484848)
    val textLight = Color(0xFF767676)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = cardWhite),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "GA Parameters",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = textDark
            )
            
            // Population Size
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Population Size",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = textDark
                    )
                    Text(
                        text = "$populationSize",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = airbnbRed
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = populationSize.toFloat(),
                    onValueChange = { onPopulationSizeChange(it.toInt()) },
                    valueRange = 50f..200f,
                    steps = 14,
                    colors = SliderDefaults.colors(
                        thumbColor = airbnbRed,
                        activeTrackColor = airbnbRed,
                        inactiveTrackColor = textLight.copy(alpha = 0.3f)
                    )
                )
            }
            
            // Crossover Rate
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Crossover Rate",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = textDark
                    )
                    Text(
                        text = String.format("%.2f", crossoverRate),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = airbnbRed
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = crossoverRate,
                    onValueChange = onCrossoverRateChange,
                    valueRange = 0f..1f,
                    steps = 19,
                    colors = SliderDefaults.colors(
                        thumbColor = airbnbRed,
                        activeTrackColor = airbnbRed,
                        inactiveTrackColor = textLight.copy(alpha = 0.3f)
                    )
                )
            }
            
            // Mutation Rate
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mutation Rate",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = textDark
                    )
                    Text(
                        text = String.format("%.2f", mutationRate),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = airbnbRed
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = mutationRate,
                    onValueChange = onMutationRateChange,
                    valueRange = 0f..1f,
                    steps = 19,
                    colors = SliderDefaults.colors(
                        thumbColor = airbnbRed,
                        activeTrackColor = airbnbRed,
                        inactiveTrackColor = textLight.copy(alpha = 0.3f)
                    )
                )
            }
        }
    }
}

@Composable
private fun OptimizationTypeCard(
    selectedType: OptimizationType,
    onTypeSelected: (OptimizationType) -> Unit
) {
    val airbnbRed = Color(0xFFFF5A5F)
    val cardWhite = Color(0xFFFFFFFF)
    val textDark = Color(0xFF484848)
    val textLight = Color(0xFF767676)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = cardWhite),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Optimization Type",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = textDark
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Distance Option
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(
                            elevation = 2.dp,
                            shape = RoundedCornerShape(12.dp),
                            ambientColor = Color.Black.copy(alpha = 0.1f),
                            spotColor = Color.Black.copy(alpha = 0.1f)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedType == OptimizationType.DISTANCE) 
                            airbnbRed.copy(alpha = 0.15f) else cardWhite
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    onClick = { onTypeSelected(OptimizationType.DISTANCE) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Distance",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedType == OptimizationType.DISTANCE) airbnbRed else textDark
                        )
                    }
                }
                
                // Time Option
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(
                            elevation = 2.dp,
                            shape = RoundedCornerShape(12.dp),
                            ambientColor = Color.Black.copy(alpha = 0.1f),
                            spotColor = Color.Black.copy(alpha = 0.1f)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedType == OptimizationType.TIME) 
                            airbnbRed.copy(alpha = 0.15f) else cardWhite
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    onClick = { onTypeSelected(OptimizationType.TIME) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Time",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedType == OptimizationType.TIME) airbnbRed else textDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteResultCard(result: TSPResult) {
    val textDark = Color(0xFF484848)
    val successGreen = Color(0xFF00A699)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = successGreen.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = successGreen,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Route Calculated",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textDark
                )
            }
            
            Divider()
            
            Text(
                text = "Total ${if (result.optimizationType == OptimizationType.DISTANCE) "Distance" else "Time"}: ${String.format("%.2f", result.distance)} ${if (result.optimizationType == OptimizationType.DISTANCE) "km" else "s"}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = textDark
            )
            
            Text(
                text = "Route (${result.route.size} locations):",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = textDark
            )
            
            Text(
                text = result.route.joinToString(" → "),
                style = MaterialTheme.typography.bodySmall,
                color = textDark
            )
        }
    }
}

