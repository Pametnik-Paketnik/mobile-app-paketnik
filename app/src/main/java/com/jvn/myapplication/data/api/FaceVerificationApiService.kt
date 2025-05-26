package com.jvn.myapplication.data.api

import com.jvn.myapplication.data.model.FaceVerificationRequest
import com.jvn.myapplication.data.model.FaceVerificationResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface FaceVerificationApiService {
    @POST("api/auth/face-verification")
    suspend fun submitFaceVerification(
        @Header("Authorization") token: String,
        @Body request: FaceVerificationRequest
    ): Response<FaceVerificationResponse>
}