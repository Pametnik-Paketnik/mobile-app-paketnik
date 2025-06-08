package com.jvn.myapplication.data.model

import com.google.gson.annotations.SerializedName

data class Reservation(
    val id: Int,
    val status: String, // e.g., "PENDING", "CHECKED_IN", "CHECKED_OUT", "CANCELLED"
    @SerializedName("checkinAt") val checkinAt: String,
    @SerializedName("checkoutAt") val checkoutAt: String,
    val guest: ReservationUser?,
    val host: ReservationUser?,
    val box: ReservationBox?
)

data class ReservationUser(
    val id: Int,
    val username: String,
    val userType: String,
    val createdAt: String?,
    val updatedAt: String?
)

data class ReservationBox(
    val boxId: String,
    val location: String?,
    val status: String?
)

data class CheckInRequest(
    val reservationId: Int
)

data class CheckInResponse(
    val success: Boolean,
    val message: String,
    val reservationId: Int?,
    val boxId: String?,
    val status: String?,
    val data: String?
)

data class CheckOutRequest(
    val reservationId: Int
)

data class CheckOutResponse(
    val success: Boolean,
    val message: String,
    val reservationId: Int?,
    val boxId: String?,
    val status: String?
) 