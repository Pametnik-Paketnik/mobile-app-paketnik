package com.jvn.myapplication.ui.screens

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import algorithms.GA
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.jvn.myapplication.BuildConfig
import com.jvn.myapplication.data.api.NetworkModule
import com.jvn.myapplication.data.model.Location
import com.jvn.myapplication.utils.AssetReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import problems.TSP
import java.io.InputStream

class MapViewModel(private val context: Context) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()
    
    private var allLocations: List<Location> = emptyList()

    fun calculateRoute(
        selectedLocationIds: List<Int>,
        optimizationType: OptimizationType,
        populationSize: Int,
        crossoverRate: Double,
        mutationRate: Double
    ) {
        if (selectedLocationIds.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please select at least one location"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                result = null
            )

            try {
                val result = withContext(Dispatchers.IO) {
                    // 1. Load locations if not already loaded
                    if (allLocations.isEmpty()) {
                        allLocations = AssetReader.readLocationsFromAssets(context)
                    }
                    
                    // 2. Load TSP file from assets
                    val fileName = when (optimizationType) {
                        OptimizationType.DISTANCE -> "direct4me_distance.tsp"
                        OptimizationType.TIME -> "direct4me_time.tsp"
                    }
                    
                    val inputStream: InputStream = context.assets.open(fileName)
                    
                    // 3. Create master problem
                    val masterProblem = TSP(inputStream, 0)
                    inputStream.close()
                    
                    // 4. Generate subproblem
                    val selectedIds = selectedLocationIds.map { it.toInt() }
                    val subProblem = masterProblem.generateSubproblem(selectedIds)
                    
                    // 5. Run GA algorithm
                    val ga = GA(populationSize, crossoverRate, mutationRate)
                    val tour = ga.execute(subProblem)
                    
                    // 6. Convert result to list of location IDs and get coordinates
                    val route = tour.path.map { it.realId }.toList()
                    val routeLocations = route.mapNotNull { id ->
                        allLocations.find { it.id == id }
                    }
                    
                    // 7. Get directions from Google Directions API
                    val directionsResult = getDirections(routeLocations)
                    
                    TSPResult(
                        route = route,
                        routeLocations = routeLocations,
                        distance = tour.distance,
                        optimizationType = optimizationType,
                        polylinePoints = directionsResult?.polylinePoints ?: "",
                        totalDistanceKm = directionsResult?.totalDistanceKm ?: 0.0,
                        totalTimeSeconds = directionsResult?.totalTimeSeconds ?: 0
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    result = result
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error calculating route: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearResult() {
        _uiState.value = _uiState.value.copy(result = null)
    }
    
    private suspend fun getDirections(locations: List<Location>): DirectionsResult? {
        if (locations.size < 2) return null
        
        return try {
            // Google Directions API allows max 25 waypoints (plus origin and destination)
            // So we can have max 23 intermediate waypoints per request
            val maxWaypointsPerRequest = 23
            val allPolylines = mutableListOf<String>()
            var totalDistanceMeters = 0
            var totalTimeSeconds = 0
            
            // Split locations into chunks
            var currentIndex = 0
            while (currentIndex < locations.size - 1) {
                val chunkEnd = minOf(currentIndex + maxWaypointsPerRequest + 1, locations.size - 1)
                val chunk = locations.subList(currentIndex, chunkEnd + 1)
                
                val origin = "${chunk.first().latitude},${chunk.first().longitude}"
                val destination = "${chunk.last().latitude},${chunk.last().longitude}"
                
                // Build waypoints string (skip first and last)
                val waypoints = if (chunk.size > 2) {
                    chunk.drop(1).dropLast(1)
                        .joinToString("|") { "${it.latitude},${it.longitude}" }
                } else {
                    ""
                }
                
                val response = NetworkModule.directionsApi.getDirections(
                    origin = origin,
                    destination = destination,
                    waypoints = waypoints,
                    key = BuildConfig.GOOGLE_API_KEY
                )
                
                if (response.status == "OK" && !response.routes.isNullOrEmpty()) {
                    val route = response.routes.first()
                    val polyline = route.overviewPolyline?.points ?: ""
                    if (polyline.isNotEmpty()) {
                        allPolylines.add(polyline)
                    }
                    
                    // Calculate total distance and time from legs
                    route.legs?.forEach { leg ->
                        totalDistanceMeters += leg.distance?.value ?: 0
                        totalTimeSeconds += leg.duration?.value ?: 0
                    }
                } else {
                    // If one chunk fails, we still try to continue with others
                    android.util.Log.w("MapViewModel", "Failed to get directions for chunk: ${response.status}")
                }
                
                // Move to next chunk (overlap by 1 to ensure continuity)
                currentIndex = chunkEnd
            }
            
            // Combine all polylines into one
            // Note: We can't directly combine encoded polylines, so we'll decode and re-encode
            // For simplicity, we'll just use the first polyline or combine decoded points
            if (allPolylines.isNotEmpty()) {
                // Decode all polylines and combine them
                val allPoints = mutableListOf<com.google.android.gms.maps.model.LatLng>()
                allPolylines.forEach { encodedPolyline ->
                    try {
                        val decoded = com.google.maps.android.PolyUtil.decode(encodedPolyline)
                        allPoints.addAll(decoded)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                // Re-encode combined polyline
                val combinedPolyline = if (allPoints.isNotEmpty()) {
                    com.google.maps.android.PolyUtil.encode(allPoints)
                } else {
                    allPolylines.firstOrNull() ?: ""
                }
                
                DirectionsResult(
                    polylinePoints = combinedPolyline,
                    totalDistanceKm = totalDistanceMeters / 1000.0,
                    totalTimeSeconds = totalTimeSeconds
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

data class DirectionsResult(
    val polylinePoints: String,
    val totalDistanceKm: Double,
    val totalTimeSeconds: Int
)

data class MapUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val result: TSPResult? = null
)

data class TSPResult(
    val route: List<Int>,
    val routeLocations: List<Location>,
    val distance: Double,
    val optimizationType: OptimizationType,
    val polylinePoints: String = "",
    val totalDistanceKm: Double = 0.0,
    val totalTimeSeconds: Int = 0
)

enum class OptimizationType {
    DISTANCE,
    TIME
}

