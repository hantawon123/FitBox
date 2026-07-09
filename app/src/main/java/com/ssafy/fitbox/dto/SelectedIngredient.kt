package com.ssafy.fitbox.dto

data class SelectedIngredient(
    val ingredient: Ingredient,
    val quantityGram: Int
) {
    fun increase(amountGram: Int = DEFAULT_QUANTITY_STEP): SelectedIngredient {
        return copy(quantityGram = quantityGram + amountGram)
    }

    fun decrease(amountGram: Int = DEFAULT_QUANTITY_STEP): SelectedIngredient {
        val newQuantityGram = quantityGram - amountGram
        return copy(quantityGram = newQuantityGram.coerceAtLeast(0))
    }

    fun isEmpty(): Boolean {
        return quantityGram <= 0
    }

    fun totalCalories(): Double {
        return ingredient.calories * quantityGram
    }

    fun totalCarbohydrate(): Double {
        return ingredient.carbohydrate * quantityGram
    }

    fun totalProtein(): Double {
        return ingredient.protein * quantityGram
    }

    fun totalFat(): Double {
        return ingredient.fat * quantityGram
    }

    fun totalPrice(): Int {
        return ingredient.price * quantityGram
    }

    companion object {
        const val DEFAULT_QUANTITY_STEP = 50
    }
}
