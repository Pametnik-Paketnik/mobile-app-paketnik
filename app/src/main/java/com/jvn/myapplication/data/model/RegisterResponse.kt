package com.jvn.myapplication.data.model

data class RegisterResponse(
    val success: Boolean,
    val message: String,
    val access_token: String,
    val user: User
)