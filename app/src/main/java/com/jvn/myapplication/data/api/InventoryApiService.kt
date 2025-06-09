package com.jvn.myapplication.data.api

import com.jvn.myapplication.data.model.InventoryItem
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface InventoryApiService {
    @GET("api/inventory-items/host/{hostId}")
    suspend fun getInventoryByHost(
        @Path("hostId") hostId: Int,
        @Header("Authorization") token: String
    ): Response<List<InventoryItem>>
} 