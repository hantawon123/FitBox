package com.ssafy.fitbox.dto

enum class IngredientCategory(
    val displayName: String
) {
    PROTEIN("단백질"),
    CARBOHYDRATE("탄수화물"),
    VEGETABLE("채소"),
    SAUCE("소스"),
    UNKNOWN("기타");

    companion object {
        fun fromServerValue(value: String?): IngredientCategory {
            return when (value?.uppercase()) {
                "PROTEIN" -> PROTEIN
                "CARBOHYDRATE", "CARB", "BASE" -> CARBOHYDRATE
                "VEGETABLE" -> VEGETABLE
                "SAUCE" -> SAUCE
                else -> UNKNOWN
            }
        }
    }
}