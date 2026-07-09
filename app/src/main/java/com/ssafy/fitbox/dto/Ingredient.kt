package com.ssafy.fitbox.dto

data class Ingredient(
    val id: Int,
    val name: String,
    val category: IngredientCategory,
    val calories: Double,
    val carbohydrate: Double,
    val protein: Double,
    val fat: Double,
    val price: Int,
    val imageUrl : String?
)