package com.jvn.myapplication.data.model

import com.google.gson.annotations.SerializedName

data class BoxAvailabilityResponse(
    val boxId: String,
    val unavailableDates: List<UnavailableDateRange>
)

data class UnavailableDateRange(
    val startDate: String, // ISO date string
    val endDate: String,   // ISO date string
    val status: String     // e.g., "BOOKED", "MAINTENANCE", etc.
)

data class DateRange(
    val startDate: String,
    val endDate: String
) 