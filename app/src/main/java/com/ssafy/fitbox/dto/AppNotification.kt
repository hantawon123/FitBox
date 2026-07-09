package com.ssafy.fitbox.dto

data class AppNotification(
    val id: Long,
    val userId: Int,
    val title: String,
    val message: String,
    val read: Boolean = false,
    val createdAt: String
)
