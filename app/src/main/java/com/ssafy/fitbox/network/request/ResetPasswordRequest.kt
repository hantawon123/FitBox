package com.ssafy.fitbox.network.request

data class ResetPasswordRequest(
    val userId: String,
    val name: String,
    val phone: String,
    val newPassword: String
)
