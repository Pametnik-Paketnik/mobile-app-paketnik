// File: data/api/AuthApiService.kt
package com.jvn.myapplication.data.api

import com.jvn.myapplication.data.model.LoginRequest
import com.jvn.myapplication.data.model.LoginResponse
import com.jvn.myapplication.data.model.LogoutResponse
import com.jvn.myapplication.data.model.RegisterRequest
import com.jvn.myapplication.data.model.RegisterResponse
import com.jvn.myapplication.data.model.UserUpdateRequest
import com.jvn.myapplication.data.model.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.Path

interface AuthApiService {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("api/auth/logout")
    suspend fun logout(@Header("Authorization") token: String): Response<LogoutResponse>

    // Push notification endpoints for 2FA
    @POST("api/auth/update-fcm-token")
    suspend fun updateFcmToken(
        @Header("Authorization") token: String,
        @Body request: Map<String, String>
    ): Response<Map<String, Any>>

    @POST("api/auth/approve-pending-auth")
    suspend fun approvePendingAuth(
        @Header("Authorization") token: String,
        @Body request: Map<String, String>
    ): Response<Map<String, Any>>

    @POST("api/auth/deny-pending-auth")
    suspend fun denyPendingAuth(
        @Header("Authorization") token: String,
        @Body request: Map<String, String>
    ): Response<Map<String, Any>>

    @PATCH("api/users/{id}")
    suspend fun updateUser(
        @Path("id") userId: Int,
        @Header("Authorization") token: String,
        @Body request: UserUpdateRequest
    ): Response<User>
}