package com.jvn.myapplication.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

data class TotpSetupResponse(
    val secret: String,
    val qrCodeUri: String,
    val manualEntryKey: String
)

data class TotpDisableResponse(
    val success: Boolean,
    val message: String
)

data class TotpStatusResponse(
    val enabled: Boolean,
    val message: String
)

data class TotpVerifySetupRequest(
    val code: String
)

data class TotpVerifySetupResponse(
    val success: Boolean,
    val message: String
)

interface TotpApiService {
    @POST("api/2fa/totp/setup")
    suspend fun setupTotp(
        @Header("Authorization") token: String
    ): Response<TotpSetupResponse>

    @DELETE("api/2fa/totp/disable")
    suspend fun disableTotp(
        @Header("Authorization") token: String
    ): Response<TotpDisableResponse>

    @GET("api/2fa/totp/status")
    suspend fun getTotpStatus(
        @Header("Authorization") token: String
    ): Response<TotpStatusResponse>

    @POST("api/2fa/totp/verify-setup")
    suspend fun verifyTotpSetup(
        @Header("Authorization") token: String,
        @Body request: TotpVerifySetupRequest
    ): Response<TotpVerifySetupResponse>
} 