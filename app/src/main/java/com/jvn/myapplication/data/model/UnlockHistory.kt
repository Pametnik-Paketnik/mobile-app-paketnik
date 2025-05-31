
// File: data/model/UnlockHistory.kt
package com.jvn.myapplication.data.model

data class UnlockHistory(
    val user: User,
    val boxId: String,
    val timestamp: String,
    val status: String,
    val tokenFormat: Int
)