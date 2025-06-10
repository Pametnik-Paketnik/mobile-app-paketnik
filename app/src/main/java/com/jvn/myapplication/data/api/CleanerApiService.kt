package com.jvn.myapplication.data.api

import com.jvn.myapplication.data.model.ExtraOrder
import com.jvn.myapplication.data.model.FulfillOrderRequest
import com.jvn.myapplication.data.model.FulfillOrderResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.Path

interface CleanerApiService {
    // Get all pending extra orders for the cleaner using JWT token
    @GET("api/extra-orders/pending/my-orders")
    suspend fun getAllPendingExtraOrders(
        @Header("Authorization") token: String
    ): Response<List<ExtraOrder>>

    // Fulfill an order
    @PATCH("api/extra-orders/{orderId}/fulfill")
    suspend fun fulfillOrder(
        @Path("orderId") orderId: Int,
        @Header("Authorization") token: String,
        @Body request: FulfillOrderRequest
    ): Response<FulfillOrderResponse>
} 