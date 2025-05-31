// File: data/model/UnlockHistoryWithUser.kt
package com.jvn.myapplication.data.model

data class UnlockHistoryWithUser(
    val id: Int,
    val boxId: String,
    val timestamp: String,
    val status: String,
    val tokenFormat: Int,
    val userId: Int,
    val username: String? = null
)