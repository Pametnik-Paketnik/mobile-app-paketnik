package com.jvn.myapplication.data.api

import com.jvn.myapplication.config.ApiConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authApi: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }

    val faceVerificationApi: FaceVerificationApiService by lazy {
        retrofit.create(FaceVerificationApiService::class.java)
    }

    val boxApi: BoxApiService by lazy {
        retrofit.create(BoxApiService::class.java)
    }

    val reservationApi: ReservationApiService by lazy {
        retrofit.create(ReservationApiService::class.java)
    }

    val faceAuthApi: FaceAuthApiService by lazy {
        retrofit.create(FaceAuthApiService::class.java)
    }
}