// In your data/model/RegisterRequest.kt file:
package com.jvn.myapplication.data.model

data class RegisterRequest(
    val username: String,
    val password: String,
    val userType: String = "USER" // Default to USER, but can be "HOST"
)