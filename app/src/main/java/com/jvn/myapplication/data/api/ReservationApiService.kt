package com.jvn.myapplication.data.api

import com.jvn.myapplication.data.model.CheckInRequest
import com.jvn.myapplication.data.model.CheckInResponse
import com.jvn.myapplication.data.model.CheckOutRequest
import com.jvn.myapplication.data.model.CheckOutResponse
import com.jvn.myapplication.data.model.Reservation
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ReservationApiService {
    @GET("api/reservations")
    suspend fun getAllReservations(
        @Header("Authorization") token: String
    ): Response<List<Reservation>>

    @GET("api/reservations/guest/{guestId}")
    suspend fun getReservationsByGuest(
        @Path("guestId") guestId: String,
        @Header("Authorization") token: String
    ): Response<List<Reservation>>

    @GET("api/reservations/host/{hostId}")
    suspend fun getReservationsByHost(
        @Path("hostId") hostId: String,
        @Header("Authorization") token: String
    ): Response<List<Reservation>>

    @GET("api/reservations/{id}")
    suspend fun getReservationById(
        @Path("id") id: String,
        @Header("Authorization") token: String
    ): Response<Reservation>

    @POST("api/reservations/checkin")
    suspend fun checkIn(
        @Header("Authorization") token: String,
        @Body request: CheckInRequest
    ): Response<CheckInResponse>

    @POST("api/reservations/checkout")
    suspend fun checkOut(
        @Header("Authorization") token: String,
        @Body request: CheckOutRequest
    ): Response<CheckOutResponse>
} 