package com.jvn.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.maps.android.compose.*
import com.google.maps.android.PolyUtil
import com.jvn.myapplication.data.model.Location
import androidx.compose.ui.Alignment

@Composable
fun RouteMapView(
    result: TSPResult,
    modifier: Modifier = Modifier
) {
    val airbnbRed = Color(0xFFFF5A5F)
    val cardWhite = Color(0xFFFFFFFF)
    val textDark = Color(0xFF484848)
    
    // Calculate center of all locations for initial camera position
    val centerLat = result.routeLocations.map { it.latitude }.average()
    val centerLng = result.routeLocations.map { it.longitude }.average()
    val center = LatLng(centerLat, centerLng)
    
    // Calculate bounds for camera
    val minLat = result.routeLocations.minOfOrNull { it.latitude } ?: centerLat
    val maxLat = result.routeLocations.maxOfOrNull { it.latitude } ?: centerLat
    val minLng = result.routeLocations.minOfOrNull { it.longitude } ?: centerLng
    val maxLng = result.routeLocations.maxOfOrNull { it.longitude } ?: centerLng
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(center, 10f)
    }
    
    // Decode polyline
    val polylinePoints = remember(result.polylinePoints) {
        if (result.polylinePoints.isNotEmpty()) {
            try {
                PolyUtil.decode(result.polylinePoints)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    
    // Fit camera to bounds when locations are available
    LaunchedEffect(result.routeLocations) {
        if (result.routeLocations.isNotEmpty()) {
            val points = result.routeLocations.map { 
                LatLng(it.latitude, it.longitude) 
            }
            val bounds = LatLngBounds.Builder().apply {
                points.forEach { include(it) }
            }.build()
            
            val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 100)
            cameraPositionState.animate(cameraUpdate)
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            // Draw polyline
            if (polylinePoints.isNotEmpty()) {
                Polyline(
                    points = polylinePoints,
                    color = airbnbRed,
                    width = 8f
                )
            }
            
            // Add markers for each location
            result.routeLocations.forEachIndexed { index, location ->
                Marker(
                    state = MarkerState(position = LatLng(location.latitude, location.longitude)),
                    title = location.address,
                    snippet = "Stop ${index + 1}",
                    icon = BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_RED
                    )
                )
            }
        }
        
        // Info card at the bottom
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(androidx.compose.ui.Alignment.BottomCenter),
            colors = CardDefaults.cardColors(containerColor = cardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Route Summary",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textDark
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Total Distance",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textDark
                        )
                        Text(
                            text = String.format("%.2f km", result.totalDistanceKm),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = airbnbRed
                        )
                    }
                    
                    Column {
                        Text(
                            text = "Total Time",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textDark
                        )
                        Text(
                            text = formatTime(result.totalTimeSeconds),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = airbnbRed
                        )
                    }
                }
                
                Text(
                    text = "${result.routeLocations.size} locations",
                    style = MaterialTheme.typography.bodySmall,
                    color = textDark
                )
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    
    return when {
        hours > 0 -> String.format("%dh %dm", hours, minutes)
        minutes > 0 -> String.format("%dm %ds", minutes, secs)
        else -> String.format("%ds", secs)
    }
}

