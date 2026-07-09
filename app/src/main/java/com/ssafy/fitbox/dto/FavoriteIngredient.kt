package com.ssafy.fitbox.dto

data class FavoriteIngredient(
    val name: String,
    val amount: Int,
    val calories: Double = 0.0
)
