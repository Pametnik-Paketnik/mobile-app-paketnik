// File: data/api/AuthApiService.kt
package com.jvn.myapplication.data.api

import com.jvn.myapplication.data.model.LoginRequest
import com.jvn.myapplication.data.model.LoginResponse
import com.jvn.myapplication.data.model.LogoutResponse
import com.jvn.myapplication.data.model.RegisterRequest
import com.jvn.myapplication.data.model.RegisterResponse
import com.jvn.myapplication.data.model.UserUpdateRequest
import com.jvn.myapplication.data.model.User
import com.jvn.myapplication.data.model.TotpLoginRequest
import com.jvn.myapplication.data.model.TotpLoginResponse
import com.jvn.myapplication.data.model.FaceLoginResponse
import com.jvn.myapplication.data.model.DeviceRegistrationRequest
import com.jvn.myapplication.data.model.DeviceRegistrationResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.Part
import retrofit2.http.Path

interface AuthApiService {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("api/auth/logout")
    suspend fun logout(@Header("Authorization") token: String): Response<LogoutResponse>

    // Device registration for 2FA notifications
    @POST("api/users/devices")
    suspend fun registerDevice(
        @Header("Authorization") token: String,
        @Body request: DeviceRegistrationRequest
    ): Response<DeviceRegistrationResponse>

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

    // 2FA Login endpoints
    @POST("api/auth/2fa/totp/login")
    suspend fun totpLogin(@Body request: TotpLoginRequest): Response<TotpLoginResponse>

    @Multipart
    @POST("api/auth/2fa/face/login")
    suspend fun faceLogin(
        @Part("tempToken") tempToken: RequestBody,
        @Part face_image: MultipartBody.Part
    ): Response<FaceLoginResponse>
}