package com.ssafy.fitbox.dto

import java.io.Serializable

data class User(
    val id: Int = 0,
    val userId: String,
    val password: String,
    val name: String,
    val phone: String,
    val gender: String,
    val age: Int,
    val height: Double = 0.0,
    val weight: Double = 0.0,
    val activityLevel: Int = 0,
    val purpose: String,
    val allergies: List<String> = emptyList()
) : Serializable