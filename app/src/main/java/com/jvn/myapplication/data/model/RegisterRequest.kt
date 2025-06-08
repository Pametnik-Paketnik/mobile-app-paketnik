// In your data/model/RegisterRequest.kt file:
package com.jvn.myapplication.data.model

data class RegisterRequest(
    val name: String,
    val surname: String,
    val email: String,
    val password: String,
    val userType: String = "USER" // Default to USER, but can be "HOST"
)