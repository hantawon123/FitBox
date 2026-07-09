package com.ssafy.fitbox.network.response

import com.ssafy.fitbox.dto.Ingredient
import com.ssafy.fitbox.dto.IngredientCategory

data class IngredientResponse(
    val id: Int,
    val name: String,
    val calories: Double,
    val carbohydrate: Double,
    val protein: Double,
    val fat: Double,
    val categories: String?,
    val price: Int,
    val imageUrl : String?
) {
    fun toIngredient(): Ingredient {
        return Ingredient(
            id = id,
            name = name,
            category = IngredientCategory.fromServerValue(categories),
            calories = calories,
            carbohydrate = carbohydrate,
            protein = protein,
            fat = fat,
            price = price,
            imageUrl = imageUrl
        )
    }
}