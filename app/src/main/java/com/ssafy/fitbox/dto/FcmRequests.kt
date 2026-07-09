package com.ssafy.fitbox.dto

data class FcmTokenRequest(
    val userId: Int,
    val token: String
)

data class PushNotificationRequest(
    val userId: Int,
    val title: String,
    val message: String
)
