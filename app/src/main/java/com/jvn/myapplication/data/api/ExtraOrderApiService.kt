package com.jvn.myapplication.data.api

import com.jvn.myapplication.data.model.ExtraOrder
import com.jvn.myapplication.data.model.CreateExtraOrderRequest
import com.jvn.myapplication.data.model.CreateExtraOrderResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ExtraOrderApiService {
    @GET("api/extra-orders/reservation/{reservationId}")
    suspend fun getExtraOrdersByReservation(
        @Path("reservationId") reservationId: Int,
        @Header("Authorization") token: String
    ): Response<List<ExtraOrder>>

    @POST("api/extra-orders")
    suspend fun createExtraOrder(
        @Header("Authorization") token: String,
        @Body request: CreateExtraOrderRequest
    ): Response<CreateExtraOrderResponse>
} 