package com.ssafy.fitbox.dto

data class CartItem(
    val id: Long,
    val mealId: Long,
    val name: String,
    val mealType: String,
    val calories: Double,
    val carbohydrate: Double,
    val protein: Double,
    val fat: Double,
    val price: Int,
    val quantity: Int = 1,
    val imageUrl: String? = null
)

object MealType {
    const val PRODUCT = "PRODUCT"
    const val CUSTOM = "CUSTOM"
}