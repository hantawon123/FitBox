package com.ssafy.fitbox.dto

data class FavoriteMeal(
    val id: String,
    val userId: Int,
    val mealId: Long?,
    val name: String,
    val sourceType: String,
    val calories: Double,
    val carbohydrate: Double,
    val protein: Double,
    val fat: Double,
    val price: Int,
    val ingredients: List<FavoriteIngredient> = emptyList(),
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val SOURCE_ORDER = "ORDER"
        const val SOURCE_AI = "AI_RECOMMENDATION"
        const val SOURCE_CUSTOM = "CUSTOM"
    }
}
