package com.jvn.myapplication.data.model

import com.google.gson.annotations.SerializedName

data class ExtraOrder(
    val id: Int,
    val reservation: ReservationDetail,
    val items: List<ExtraOrderItem>,
    val totalPrice: String,
    val status: String, // e.g., "PENDING", "CONFIRMED", "DELIVERED", "FULFILLED"
    val fulfilledBy: User?,
    val fulfilledAt: String?,
    val notes: String?,
    val createdAt: String,
    val updatedAt: String
)

data class ReservationDetail(
    val id: Int,
    val guest: User,
    val host: User,
    val checkinAt: String,
    val checkoutAt: String,
    val actualCheckinAt: String?,
    val actualCheckoutAt: String?,
    val status: String,
    val totalPrice: String
)

data class ExtraOrderItem(
    val id: Int,
    val inventoryItem: InventoryItemDetail,
    val quantity: Int,
    val unitPrice: String,
    val totalPrice: String,
    val createdAt: String,
    val updatedAt: String
)

data class InventoryItemDetail(
    val id: Int,
    val name: String,
    val description: String?,
    val price: String,
    val isAvailable: Boolean,
    val stockQuantity: Int,
    val host: User,
    val createdAt: String,
    val updatedAt: String
)

data class ExtraOrderResponse(
    val extraOrders: List<ExtraOrder>
) 