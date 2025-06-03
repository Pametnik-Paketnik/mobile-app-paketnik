package com.jvn.myapplication.data.model

import com.google.gson.annotations.SerializedName

data class BoxData(
    val id: Int,
    @SerializedName("boxId") val boxId: String,
    val location: String,
    val status: String, // e.g., "FREE", "BUSY"
    val hostId: Int,
    val createdAt: String?,
    val updatedAt: String?
) 