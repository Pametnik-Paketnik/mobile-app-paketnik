package com.jvn.myapplication.data.model

import com.google.gson.annotations.SerializedName

data class InventoryItem(
    val id: Int,
    val name: String,
    val description: String?,
    val price: String,
    @SerializedName("stockQuantity")
    val availableQuantity: Int,
    val isAvailable: Boolean,
    val host: Host?,
    val createdAt: String?,
    val updatedAt: String?,
    val imageUrl: String? = null
)

data class Host(
    val id: Int,
    val name: String,
    val surname: String,
    val email: String,
    val userType: String,
    val twoFactorEnabled: Boolean,
    val createdAt: String,
    val updatedAt: String
)

data class CreateExtraOrderRequest(
    val reservationId: Int,
    val items: List<OrderItem>,
    val notes: String? = null
)

data class OrderItem(
    val inventoryItemId: Int,
    val quantity: Int
)

data class CreateExtraOrderResponse(
    val success: Boolean,
    val message: String,
    val orderId: Int?
) 