package com.jvn.myapplication.data.model

import com.google.gson.annotations.SerializedName

data class ExtraOrder(
    val id: Int,
    val reservationId: Int,
    val itemName: String,
    val quantity: Int,
    val price: Double,
    val status: String, // e.g., "PENDING", "CONFIRMED", "DELIVERED"
    val orderedAt: String,
    val deliveredAt: String?,
    val notes: String?
)

data class ExtraOrderResponse(
    val extraOrders: List<ExtraOrder>
) 