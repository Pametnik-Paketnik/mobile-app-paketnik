package com.jvn.myapplication.data.repository

import android.content.Context
import com.jvn.myapplication.data.api.NetworkModule
import com.jvn.myapplication.data.model.ExtraOrder
import com.jvn.myapplication.data.model.FulfillOrderRequest
import com.jvn.myapplication.data.model.FulfillOrderResponse
import kotlinx.coroutines.flow.first

class CleanerRepository(private val context: Context) {
    private val cleanerApi = NetworkModule.cleanerApi
    private val authRepository = AuthRepository(context)

    suspend fun getAllExtraOrders(): Result<List<ExtraOrder>> {
        return try {
            println("🔍 DEBUG - CleanerRepository: Getting all pending extra orders using JWT token")
            
            val token = authRepository.getAuthToken().first()
            if (token.isNullOrEmpty()) {
                println("🔍 DEBUG - CleanerRepository: ERROR - No authentication token")
                return Result.failure(Exception("No authentication token"))
            }

            println("🔍 DEBUG - CleanerRepository: Calling /api/extra-orders/pending/my-orders")
            
            val response = cleanerApi.getAllPendingExtraOrders("Bearer $token")
            
            if (response.isSuccessful && response.body() != null) {
                val extraOrders = response.body()!!
                println("🔍 DEBUG - CleanerRepository: Got ${extraOrders.size} pending extra orders")
                Result.success(extraOrders)
            } else {
                val errorMessage = "Failed to get pending extra orders: ${response.message()}"
                println("🔍 DEBUG - CleanerRepository: $errorMessage")
                Result.failure(Exception(errorMessage))
            }

        } catch (e: Exception) {
            println("🔍 DEBUG - CleanerRepository: Exception: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun fulfillOrder(orderId: Int, notes: String?): Result<FulfillOrderResponse> {
        return try {
            println("🔍 DEBUG - CleanerRepository: Fulfilling order $orderId with notes: $notes")
            
            val token = authRepository.getAuthToken().first()
            if (token.isNullOrEmpty()) {
                println("🔍 DEBUG - CleanerRepository: ERROR - No authentication token")
                return Result.failure(Exception("No authentication token"))
            }

            val request = FulfillOrderRequest(notes = notes)
            val response = cleanerApi.fulfillOrder(orderId, "Bearer $token", request)
            
            if (response.isSuccessful && response.body() != null) {
                val fulfillResponse = response.body()!!
                println("🔍 DEBUG - CleanerRepository: Order fulfilled successfully: ${fulfillResponse.message}")
                Result.success(fulfillResponse)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = "Failed to fulfill order: ${response.message()}"
                println("🔍 DEBUG - CleanerRepository: $errorMessage")
                println("🔍 DEBUG - Error body: $errorBody")
                Result.failure(Exception(errorMessage))
            }

        } catch (e: Exception) {
            println("🔍 DEBUG - CleanerRepository: Exception: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
} 