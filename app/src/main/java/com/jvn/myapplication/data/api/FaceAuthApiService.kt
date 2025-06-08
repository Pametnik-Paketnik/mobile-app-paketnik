package com.jvn.myapplication.data.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface FaceAuthApiService {
    @Multipart
    @POST("api/face-auth/register")
    suspend fun registerFace(
        @Header("Authorization") token: String,
        @Part images: List<MultipartBody.Part>
    ): Response<FaceAuthResponse>

    @Multipart
    @POST("api/face-auth/verify")
    suspend fun verifyFace(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part
    ): Response<FaceVerifyResponse>

    @GET("api/face-auth/status")
    suspend fun getStatus(
        @Header("Authorization") token: String
    ): Response<FaceStatusResponse>

    @DELETE("api/face-auth/delete")
    suspend fun deleteFaceData(
        @Header("Authorization") token: String
    ): Response<FaceAuthResponse>
}

data class FaceAuthResponse(
    val success: Boolean = true,
    val message: String = "",
    val user_id: String? = null,
    val status: String? = null
)

data class FaceVerifyResponse(
    val success: Boolean = true,
    val authenticated: Boolean = false,
    val probability: Float = 0.0f,
    val message: String = ""
)

data class FaceStatusResponse(
    val status: String = "",
    val message: String = ""
) 