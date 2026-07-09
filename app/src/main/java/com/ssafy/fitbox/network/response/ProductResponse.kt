package com.ssafy.fitbox.network.response

import com.ssafy.fitbox.R
import com.ssafy.fitbox.dto.Product
import com.ssafy.fitbox.dto.ProductIngredient

data class ProductIngredientResponse(
    val ingredientId: Int,
    val name: String,
    val amount: Int,
    val calories: Double
)

data class ProductResponse(
    val id: Int,
    val name: String,
    val mealType: String,
    val price: Int,
    val calories: Double,
    val carbohydrate: Double,
    val protein: Double,
    val fat: Double,
    val imageUrl: String?,
    val totalOrderCount: Int? = 0,
    val ingredients: List<ProductIngredientResponse>? = emptyList()
) {
    fun toProduct(): Product {
        val productIngredients = ingredients.orEmpty().map {
            ProductIngredient(it.ingredientId, it.name, it.amount, it.calories)
        }
        return Product(
            id = id,
            name = name,
            imageRes = R.drawable.logo_full_background_remove,
            imageUrl = imageUrl,
            calories = calories,
            carbohydrate = carbohydrate,
            protein = protein,
            fat = fat,
            price = price,
            description = if (productIngredients.isEmpty()) {
                "총 ${calories.format1()}kcal의 균형 잡힌 식단입니다."
            } else {
                productIngredients.joinToString("\n") {
                    "• ${it.name} ${it.amount}g"
                }
            },
            ingredients = productIngredients,
            mealType = mealType
        )
    }

    private fun Double.format1(): String = String.format("%.1f", this)
}
