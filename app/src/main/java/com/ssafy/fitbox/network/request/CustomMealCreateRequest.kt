package com.ssafy.fitbox.network.request

data class CustomMealCreateRequest(
    val name: String,
    val price: Int,
    val calories: Int,
    val carbohydrate: Double,
    val protein: Double,
    val fat: Double,
    val ingredients: List<CustomMealIngredientRequest>
)