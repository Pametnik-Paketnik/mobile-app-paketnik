package com.jvn.myapplication.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import algorithms.GA
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
                    // 1. Load TSP file from assets
                    val fileName = when (optimizationType) {
                        OptimizationType.DISTANCE -> "direct4me_distance.tsp"
                        OptimizationType.TIME -> "direct4me_time.tsp"
                    }
                    
                    val inputStream: InputStream = context.assets.open(fileName)
                    
                    // 2. Create master problem
                    val masterProblem = TSP(inputStream, 0)
                    inputStream.close()
                    
                    // 3. Generate subproblem
                    val selectedIds = selectedLocationIds.map { it.toInt() }
                    val subProblem = masterProblem.generateSubproblem(selectedIds)
                    
                    // 4. Run GA algorithm
                    val ga = GA(populationSize, crossoverRate, mutationRate)
                    val tour = ga.execute(subProblem)
                    
                    // 5. Convert result to list of location IDs
                    val route = tour.path.map { it.realId }.toList()
                    
                    TSPResult(
                        route = route,
                        distance = tour.distance,
                        optimizationType = optimizationType
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
}

data class MapUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val result: TSPResult? = null
)

data class TSPResult(
    val route: List<Int>,
    val distance: Double,
    val optimizationType: OptimizationType
)

enum class OptimizationType {
    DISTANCE,
    TIME
}

