// File: data/api/BoxApiService.kt
package com.jvn.myapplication.data.api

import com.jvn.myapplication.data.model.UnlockHistory
import com.jvn.myapplication.data.model.BoxData
import com.jvn.myapplication.data.model.BoxAvailabilityResponse
import com.jvn.myapplication.data.model.BoxOpenRequest
import com.jvn.myapplication.data.model.BoxOpenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface BoxApiService {
    @GET("api/boxes/opening-history")
    suspend fun getAllOpeningHistory(
        @Header("Authorization") token: String
    ): Response<List<UnlockHistory>>

    @GET("api/boxes/opening-history/box/{boxId}")
    suspend fun getOpeningHistoryByBox(
        @Path("boxId") boxId: String,
        @Header("Authorization") token: String
    ): Response<List<UnlockHistory>>

    @GET("api/boxes/opening-history/user/{userId}")
    suspend fun getUnlockHistoryByUser(
        @Path("userId") userId: String,
        @Header("Authorization") token: String
    ): Response<List<UnlockHistory>>

    @GET("api/boxes/host/{hostId}")
    suspend fun getBoxesByHost(
        @Path("hostId") hostId: String,
        @Header("Authorization") token: String
    ): Response<List<BoxData>>

    @GET("api/boxes")
    suspend fun getAllBoxes(
        @Header("Authorization") token: String
    ): Response<List<BoxData>>

    @GET("api/boxes/{boxId}/availability")
    suspend fun getBoxAvailability(
        @Path("boxId") boxId: String,
        @Header("Authorization") token: String
    ): Response<BoxAvailabilityResponse>

    @POST("api/boxes/open")
    suspend fun openBox(
        @Body request: BoxOpenRequest,
        @Header("Authorization") token: String
    ): Response<BoxOpenResponse>
}