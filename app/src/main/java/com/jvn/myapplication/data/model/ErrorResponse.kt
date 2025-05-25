package com.jvn.myapplication.data.model

data class ErrorResponse(
    val message: String,
    val error: String,
    val statusCode: Int
)