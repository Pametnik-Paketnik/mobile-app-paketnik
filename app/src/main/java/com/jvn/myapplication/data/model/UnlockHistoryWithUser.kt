// File: data/model/UnlockHistoryWithUser.kt (keep this for internal use)
package com.jvn.myapplication.data.model

data class UnlockHistoryWithUser(
    val id: Int = 3, // Add default since API doesn't provide this
    val boxId: String,
    val timestamp: String,
    val status: String,
    val tokenFormat: Int,
    val userId: Int,
    val username: String
)