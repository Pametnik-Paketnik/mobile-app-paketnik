package com.jvn.myapplication.data.repository

import android.content.Context
import com.jvn.myapplication.data.api.NetworkModule
import com.jvn.myapplication.data.model.ExtraOrder
import com.jvn.myapplication.data.model.InventoryItem
import com.jvn.myapplication.data.model.CreateExtraOrderRequest
import com.jvn.myapplication.data.model.CreateExtraOrderResponse
import com.jvn.myapplication.data.model.Reservation
import kotlinx.coroutines.flow.first

class ExtraOrderRepository(private val context: Context) {
    private val extraOrderApi = NetworkModule.extraOrderApi
    private val inventoryApi = NetworkModule.inventoryApi
    private val authRepository = AuthRepository(context)
    private val reservationRepository = ReservationRepository(context)

    suspend fun getInventoryForReservation(reservationId: Int, hostId: Int): Result<List<InventoryItem>> {
        return try {
            println("🔍 DEBUG - ExtraOrderRepository: Getting inventory for reservation $reservationId, host $hostId")
            
            val token = authRepository.getAuthToken().first()
            if (token.isNullOrEmpty()) {
                println("🔍 DEBUG - ExtraOrderRepository: ERROR - No authentication token")
                return Result.failure(Exception("No authentication token"))
            }

            println("🔍 DEBUG - ExtraOrderRepository: Getting inventory for host $hostId")
            
            // Get inventory items for this host
            val response = inventoryApi.getInventoryByHost(hostId, "Bearer $token")
            
            if (response.isSuccessful && response.body() != null) {
                val inventoryItems = response.body()!!
                println("🔍 DEBUG - ExtraOrderRepository: Got ${inventoryItems.size} inventory items")
                Result.success(inventoryItems)
            } else {
                val errorMessage = "Failed to get inventory: ${response.message()}"
                println("🔍 DEBUG - ExtraOrderRepository: $errorMessage")
                Result.failure(Exception(errorMessage))
            }

        } catch (e: Exception) {
            println("🔍 DEBUG - ExtraOrderRepository: Exception: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun createExtraOrder(
        reservationId: Int,
        items: List<Pair<Int, Int>>, // List of (inventoryItemId, quantity) pairs
        notes: String? = null
    ): Result<CreateExtraOrderResponse> {
        return try {
            println("🔍 DEBUG - ExtraOrderRepository: Creating batch extra order")
            println("🔍 DEBUG - reservationId: $reservationId, items: $items, notes: $notes")
            
            val token = authRepository.getAuthToken().first()
            if (token.isNullOrEmpty()) {
                println("🔍 DEBUG - ExtraOrderRepository: ERROR - No authentication token")
                return Result.failure(Exception("No authentication token"))
            }

            val orderItems = items.map { (inventoryItemId, quantity) ->
                com.jvn.myapplication.data.model.OrderItem(
                    inventoryItemId = inventoryItemId,
                    quantity = quantity
                )
            }

            val request = CreateExtraOrderRequest(
                reservationId = reservationId,
                items = orderItems,
                notes = notes
            )

            val response = extraOrderApi.createExtraOrder("Bearer $token", request)
            
            if (response.isSuccessful && response.body() != null) {
                val createResponse = response.body()!!
                println("🔍 DEBUG - ExtraOrderRepository: Order created successfully: ${createResponse.message}")
                Result.success(createResponse)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = "Failed to create order: ${response.message()}"
                println("🔍 DEBUG - ExtraOrderRepository: $errorMessage")
                println("🔍 DEBUG - Error body: $errorBody")
                Result.failure(Exception(errorMessage))
            }

        } catch (e: Exception) {
            println("🔍 DEBUG - ExtraOrderRepository: Exception: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
} 