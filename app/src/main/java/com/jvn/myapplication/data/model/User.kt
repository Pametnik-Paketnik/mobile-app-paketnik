package com.jvn.myapplication.data.model

data class User(
    val id: Int,
    val username: String,
    val userType: String // "USER" or "HOST"
)