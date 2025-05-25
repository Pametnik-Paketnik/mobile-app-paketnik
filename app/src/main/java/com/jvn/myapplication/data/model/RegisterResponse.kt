package com.jvn.myapplication.data.model

data class RegisterResponse(
    val success: Boolean? = null,
    val message: String,
    val access_token: String? = null
)