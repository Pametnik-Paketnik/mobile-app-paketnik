package com.jvn.myapplication.data.model

data class FulfillOrderRequest(
    val notes: String?
)

data class FulfillOrderResponse(
    val success: Boolean,
    val message: String,
    val orderId: Int?
) 