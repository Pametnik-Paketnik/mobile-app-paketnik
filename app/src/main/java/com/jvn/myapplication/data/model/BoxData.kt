package com.jvn.myapplication.data.model

import com.google.gson.annotations.SerializedName

data class BoxData(
    @SerializedName("boxId") val boxId: String?,
    val location: String?,
    val owner: BoxOwner?,
    val images: List<BoxImage>?,
    val pricePerNight: String?,
    val status: String? = null // This might not be in the API response, keeping for backward compatibility
)

data class BoxOwner(
    val id: Int,
    val username: String?,
    val userType: String?,
    val createdAt: String?,
    val updatedAt: String?
)

data class BoxImage(
    val id: Int,
    val imageKey: String?,
    val fileName: String?,
    val mimeType: String?,
    val fileSize: Int?,
    val imageUrl: String?,
    val isPrimary: Boolean?,
    val createdAt: String?
) 