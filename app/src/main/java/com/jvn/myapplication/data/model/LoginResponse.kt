package com.jvn.myapplication.data.model

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val access_token: String? = null
)