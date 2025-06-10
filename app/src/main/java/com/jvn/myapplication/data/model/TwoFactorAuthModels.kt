package com.jvn.myapplication.data.model

// TOTP 2FA Models
data class TotpLoginRequest(
    val tempToken: String,
    val code: String
)

data class TotpLoginResponse(
    val success: Boolean,
    val message: String,
    val access_token: String,
    val twoFactorRequired: Boolean,
    val tempToken: String? = null,
    val available_2fa_methods: List<TwoFactorMethod>? = null,
    val user: User
)

// Face 2FA Models  
data class FaceLoginResponse(
    val success: Boolean,
    val message: String,
    val access_token: String,
    val twoFactorRequired: Boolean,
    val tempToken: String? = null,
    val available_2fa_methods: List<TwoFactorMethod>? = null,
    val user: User
) 