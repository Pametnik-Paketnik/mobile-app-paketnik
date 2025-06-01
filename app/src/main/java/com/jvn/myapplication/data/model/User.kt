// File: data/model/User.kt (make sure this matches)
package com.jvn.myapplication.data.model

data class User(
    val id: Int,
    val username: String,
    val userType: String,
    val createdAt: String? = null,
    val updatedAt: String? = null
)