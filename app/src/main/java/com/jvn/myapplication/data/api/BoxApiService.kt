// File: data/api/BoxApiService.kt
package com.jvn.myapplication.data.api

import com.jvn.myapplication.data.model.UnlockHistory
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
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
}