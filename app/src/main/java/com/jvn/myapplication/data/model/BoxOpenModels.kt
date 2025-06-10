package com.jvn.myapplication.data.model

import com.google.gson.annotations.SerializedName

data class BoxOpenRequest(
    val boxId: Int
)

data class BoxOpenResponse(
    val data: String, // Base64 encoded MP3 data
    val result: Int,
    val errorNumber: Int,
    val tokenFormat: Int
) 