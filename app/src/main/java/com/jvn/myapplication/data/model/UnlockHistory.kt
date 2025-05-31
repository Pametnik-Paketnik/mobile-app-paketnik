// File: data/model/UnlockHistory.kt
package com.jvn.myapplication.data.model

data class UnlockHistory(
    val id: Int,
    val box_id: String,
    val timestamp: String,
    val status: String,
    val token_format: Int,
    val user_id: Int
)