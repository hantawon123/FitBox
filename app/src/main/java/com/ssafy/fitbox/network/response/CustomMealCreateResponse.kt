package com.ssafy.fitbox.network.response

data class CustomMealCreateResponse(
    val mealId: Long,
    val name: String,
    val price: Int,
    val calories: Int,
    val carbohydrate: Double,
    val protein: Double,
    val fat: Double
)