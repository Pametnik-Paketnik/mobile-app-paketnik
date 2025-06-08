package com.jvn.myapplication.data.model

import com.google.gson.annotations.SerializedName

data class BoxAvailabilityResponse(
    val boxId: String,
    val unavailableDates: List<String> // Array of date strings in ISO format
)

data class DateRange(
    val startDate: String,
    val endDate: String
) 