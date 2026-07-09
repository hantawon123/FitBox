package com.ssafy.fitbox.dto

data class DietReport(
    val totalCalories: Double,
    val totalCarbs: Double,
    val totalProtein: Double,
    val totalFat: Double,
    val totalPrice: Int,
    val reason: String,
    val items: List<DietItem>
)