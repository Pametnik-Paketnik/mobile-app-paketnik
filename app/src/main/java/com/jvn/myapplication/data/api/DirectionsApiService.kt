package com.jvn.myapplication.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface DirectionsApiService {
    @GET("directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("waypoints") waypoints: String? = null,
        @Query("key") key: String
    ): DirectionsResponse
}

data class DirectionsResponse(
    @SerializedName("routes") val routes: List<Route>?,
    @SerializedName("status") val status: String?
)

data class Route(
    @SerializedName("overview_polyline") val overviewPolyline: OverviewPolyline?,
    @SerializedName("legs") val legs: List<Leg>?
)

data class OverviewPolyline(
    @SerializedName("points") val points: String
)

data class Leg(
    @SerializedName("distance") val distance: Distance?,
    @SerializedName("duration") val duration: Duration?
)

data class Distance(
    @SerializedName("value") val value: Int, // in meters
    @SerializedName("text") val text: String
)

data class Duration(
    @SerializedName("value") val value: Int, // in seconds
    @SerializedName("text") val text: String
)

