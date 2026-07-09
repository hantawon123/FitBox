package com.ssafy.fitbox.dto

data class Product(
    val id: Int,
    val name: String,
    val imageRes: Int,
    val imageUrl: String? = null,
    val calories: Double,
    val carbohydrate: Double,
    val protein: Double,
    val fat: Double,
    val price: Int,
    val description: String,
    val ingredients: List<ProductIngredient> = emptyList(),
    val mealType: String = "PRODUCT"
)
