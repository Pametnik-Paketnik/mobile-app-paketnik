package com.jvn.myapplication.data.model

import com.google.gson.annotations.SerializedName

/**
 * Request model for device registration
 * Used to register a device for 2FA notifications
 */
data class DeviceRegistrationRequest(
    @SerializedName("fcmToken")
    val fcmToken: String,
    
    @SerializedName("platform") 
    val platform: String, // "android", "ios", "web"
    
    @SerializedName("deviceName")
    val deviceName: String? = null, // optional
    
    @SerializedName("deviceId") 
    val deviceId: String? = null, // optional
    
    @SerializedName("appVersion")
    val appVersion: String? = null // optional
)

/**
 * Response model for device registration
 */
data class DeviceRegistrationResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("deviceId")
    val deviceId: String? = null
) 