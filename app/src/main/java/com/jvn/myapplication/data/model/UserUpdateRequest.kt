package com.jvn.myapplication.data.model

data class UserUpdateRequest(
    val name: String,
    val surname: String,
    val email: String,
    val password: String,
    val userType: String
)

// The API returns the user object directly, not wrapped in a response
// So we'll use the User model directly 