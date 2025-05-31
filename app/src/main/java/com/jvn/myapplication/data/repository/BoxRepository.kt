// File: data/repository/BoxRepository.kt
package com.jvn.myapplication.data.repository

import android.content.Context
import com.jvn.myapplication.data.api.NetworkModule
import com.jvn.myapplication.data.model.UnlockHistory
import com.jvn.myapplication.data.model.UnlockHistoryWithUser
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
                val historyItems = response.body()!!.map { item ->
                    UnlockHistoryWithUser(
                        id = item.id,
                        boxId = item.box_id,
                        timestamp = formatTimestamp(item.timestamp),
                        status = item.status,
                        tokenFormat = item.token_format,
                        userId = item.user_id,
                        username = "User ${item.user_id}" // In real app, you'd fetch username
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
                val historyItems = response.body()!!.map { item ->
                    UnlockHistoryWithUser(
                        id = item.id,
                        boxId = item.box_id,
                        timestamp = formatTimestamp(item.timestamp),
                        status = item.status,
                        tokenFormat = item.token_format,
                        userId = item.user_id,
                        username = "User ${item.user_id}"
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

    private fun formatTimestamp(timestamp: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            val date = inputFormat.parse(timestamp)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            timestamp // Return original if parsing fails
        }
    }
}