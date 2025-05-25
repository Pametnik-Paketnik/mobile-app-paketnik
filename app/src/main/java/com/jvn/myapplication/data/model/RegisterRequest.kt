package com.jvn.myapplication.data.model

data class RegisterRequest(
    val username: String,
    val password: String,
    val confirmPassword: String
)