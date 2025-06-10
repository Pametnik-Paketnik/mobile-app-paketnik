// Make sure your LoginResponse.kt looks like this:

package com.jvn.myapplication.data.model

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val access_token: String? = null,
    val user: User? = null,
    val twoFactorRequired: Boolean? = null,
    val tempToken: String? = null,
    val available_2fa_methods: List<TwoFactorMethod>? = null
)

data class TwoFactorMethod(
    val type: String, // "totp" or "face_id"
    val enabled: Boolean,
    val display_name: String // "Authenticator App" or "Face ID"
)

