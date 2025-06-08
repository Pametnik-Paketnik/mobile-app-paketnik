// File: data/repository/BoxRepository.kt
package com.jvn.myapplication.data.repository

import android.content.Context
import com.jvn.myapplication.data.api.NetworkModule
import com.jvn.myapplication.data.model.UnlockHistory
import com.jvn.myapplication.data.model.UnlockHistoryWithUser
import com.jvn.myapplication.data.model.BoxData
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class BoxRepository(private val context: Context) {
    private val boxApi = NetworkModule.boxApi
    private val authRepository = AuthRepository(context)

    suspend fun getAllUnlockHistory(): Result<List<UnlockHistoryWithUser>> {
        return try {
            val token = authRepository.getAuthToken().first()
            if (token.isNullOrEmpty()) {
                return Result.failure(Exception("No authentication token"))
            }

            val response = boxApi.getAllOpeningHistory("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                val historyItems = response.body()!!.map { item: UnlockHistory ->
                    UnlockHistoryWithUser(
                        id = 0, // API doesn't provide this for this endpoint
                        boxId = item.boxId,
                        timestamp = formatTimestamp(item.timestamp),
                        status = item.status,
                        tokenFormat = item.tokenFormat,
                        userId = item.user.id,
                        username = item.user.username
                    )
                }
                Result.success(historyItems)
            } else {
                Result.failure(Exception("Failed to fetch unlock history: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUnlockHistoryByBox(boxId: String): Result<List<UnlockHistoryWithUser>> {
        return try {
            val token = authRepository.getAuthToken().first()
            if (token.isNullOrEmpty()) {
                return Result.failure(Exception("No authentication token"))
            }

            val response = boxApi.getOpeningHistoryByBox(boxId, "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                val historyItems = response.body()!!.map { item: UnlockHistory ->
                    UnlockHistoryWithUser(
                        id = 0, // API doesn't provide this for this endpoint
                        boxId = item.boxId,
                        timestamp = formatTimestamp(item.timestamp),
                        status = item.status,
                        tokenFormat = item.tokenFormat,
                        userId = item.user.id,
                        username = item.user.username
                    )
                }
                Result.success(historyItems)
            } else {
                Result.failure(Exception("Failed to fetch unlock history: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUnlockHistoryByUser(userId: String): Result<List<UnlockHistoryWithUser>> {
        return try {
            val token = authRepository.getAuthToken().first()
            if (token.isNullOrEmpty()) {
                println("🔍 DEBUG - BoxRepository: No auth token")
                return Result.failure(Exception("No authentication token"))
            }

            println("🔍 DEBUG - BoxRepository: Making API call to /api/boxes/opening-history/user/$userId")
            println("🔍 DEBUG - BoxRepository: Using token: Bearer ${token.take(20)}...")

            val response = boxApi.getUnlockHistoryByUser(userId, "Bearer $token")

            println("🔍 DEBUG - BoxRepository: Response isSuccessful = ${response.isSuccessful}")
            println("🔍 DEBUG - BoxRepository: Response code = ${response.code()}")
            println("🔍 DEBUG - BoxRepository: Response message = ${response.message()}")

            if (response.isSuccessful && response.body() != null) {
                val rawData = response.body()!!
                println("🔍 DEBUG - BoxRepository: Raw API response contains ${rawData.size} items")

                val historyItems = rawData.map { item: UnlockHistory ->
                    println("🔍 DEBUG - BoxRepository: Processing item - boxId: ${item.boxId}, userId: ${item.user.id}, status: ${item.status}")
                    UnlockHistoryWithUser(
                        id = 0,
                        boxId = item.boxId,
                        timestamp = formatTimestamp(item.timestamp),
                        status = item.status,
                        tokenFormat = item.tokenFormat,
                        userId = item.user.id,
                        username = "You"
                    )
                }
                println("🔍 DEBUG - BoxRepository: Returning ${historyItems.size} processed items")
                Result.success(historyItems)
            } else {
                val errorBody = response.errorBody()?.string()
                println("🔍 DEBUG - BoxRepository: API error - ${response.code()}: $errorBody")
                Result.failure(Exception("Failed to fetch your unlock history: ${response.message()}"))
            }
        } catch (e: Exception) {
            println("🔍 DEBUG - BoxRepository: Exception occurred: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getUnlockHistoryByHost(hostId: Int): Result<List<UnlockHistoryWithUser>> {
        return try {
            val token = authRepository.getAuthToken().first()
            if (token.isNullOrEmpty()) {
                return Result.failure(Exception("No authentication token"))
            }

            println("🔍 DEBUG - BoxRepository: Getting unlock history for host $hostId")
            
            // First, get all boxes owned by this host
            val boxesResponse = boxApi.getBoxesByHost(hostId.toString(), "Bearer $token")
            if (!boxesResponse.isSuccessful || boxesResponse.body() == null) {
                return Result.failure(Exception("Failed to fetch host's boxes: ${boxesResponse.message()}"))
            }
            
            val hostBoxes = boxesResponse.body()!!
            println("🔍 DEBUG - BoxRepository: Host has ${hostBoxes.size} boxes")
            
            if (hostBoxes.isEmpty()) {
                return Result.success(emptyList())
            }
            
            // Get unlock history for all boxes and filter by host's boxes
            val allHistoryResponse = boxApi.getAllOpeningHistory("Bearer $token")
            if (allHistoryResponse.isSuccessful && allHistoryResponse.body() != null) {
                val allHistory = allHistoryResponse.body()!!
                val hostBoxIds = hostBoxes.mapNotNull { it.boxId }.toSet()
                
                val filteredHistory = allHistory.filter { item ->
                    hostBoxIds.contains(item.boxId)
                }.map { item: UnlockHistory ->
                    UnlockHistoryWithUser(
                        id = 0,
                        boxId = item.boxId,
                        timestamp = formatTimestamp(item.timestamp),
                        status = item.status,
                        tokenFormat = item.tokenFormat,
                        userId = item.user.id,
                        username = item.user.username
                    )
                }
                
                println("🔍 DEBUG - BoxRepository: Returning ${filteredHistory.size} unlock history items for host")
                Result.success(filteredHistory)
            } else {
                Result.failure(Exception("Failed to fetch unlock history: ${allHistoryResponse.message()}"))
            }
        } catch (e: Exception) {
            println("🔍 DEBUG - BoxRepository: Exception in getUnlockHistoryByHost: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getBoxesByHost(hostId: Int): Result<List<BoxData>> {
        return try {
            val token = authRepository.getAuthToken().first()
            if (token.isNullOrEmpty()) {
                return Result.failure(Exception("No authentication token"))
            }

            println("🔍 DEBUG - BoxRepository: Getting boxes for host $hostId")
            
            val response = boxApi.getBoxesByHost(hostId.toString(), "Bearer $token")
            
            println("🔍 DEBUG - BoxRepository: Response code: ${response.code()}")
            println("🔍 DEBUG - BoxRepository: Response successful: ${response.isSuccessful}")
            
            if (response.isSuccessful && response.body() != null) {
                val boxes = response.body()!!
                println("🔍 DEBUG - BoxRepository: Got ${boxes.size} boxes for host")
                boxes.forEach { box ->
                    println("🔍 DEBUG - Box: BoxId=${box.boxId}, Location=${box.location}, Status=${box.status}")
                }
                Result.success(boxes)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = "Failed to fetch host's boxes: ${response.message()}"
                println("🔍 DEBUG - BoxRepository: $errorMessage")
                println("🔍 DEBUG - Error body: $errorBody")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            println("🔍 DEBUG - BoxRepository: Exception in getBoxesByHost: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getAllBoxes(): Result<List<BoxData>> {
        return try {
            val token = authRepository.getAuthToken().first()
            if (token.isNullOrEmpty()) {
                return Result.failure(Exception("No authentication token"))
            }

            println("🔍 DEBUG - BoxRepository: Getting all boxes")
            
            val response = boxApi.getAllBoxes("Bearer $token")
            
            println("🔍 DEBUG - BoxRepository: Response code: ${response.code()}")
            println("🔍 DEBUG - BoxRepository: Response successful: ${response.isSuccessful}")
            
            if (response.isSuccessful && response.body() != null) {
                val boxes = response.body()!!
                println("🔍 DEBUG - BoxRepository: Got ${boxes.size} boxes")
                boxes.forEach { box ->
                    println("🔍 DEBUG - Box: BoxId=${box.boxId}, Location=${box.location}, Status=${box.status}")
                }
                Result.success(boxes)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = "Failed to fetch all boxes: ${response.message()}"
                println("🔍 DEBUG - BoxRepository: $errorMessage")
                println("🔍 DEBUG - Error body: $errorBody")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            println("🔍 DEBUG - BoxRepository: Exception in getAllBoxes: ${e.message}")
            Result.failure(e)
        }
    }

    private fun formatTimestamp(timestamp: String): String {
        return try {
            // Handle ISO 8601 format: "2025-05-29T11:09:51.277Z"
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(timestamp)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            // Fallback: try without milliseconds
            try {
                val fallbackFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                fallbackFormat.timeZone = TimeZone.getTimeZone("UTC")
                val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                val date = fallbackFormat.parse(timestamp)
                outputFormat.format(date ?: Date())
            } catch (e2: Exception) {
                timestamp // Return original if all parsing fails
            }
        }
    }
}