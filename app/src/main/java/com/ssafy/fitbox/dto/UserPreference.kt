package com.ssafy.fitbox.dto

data class UserPreference(
    val id: Int = 0,
    val userId: Int,
    val preferencePrompt: String
)