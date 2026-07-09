package com.ssafy.fitbox.util

import java.text.DecimalFormat
import kotlin.math.roundToInt

object DisplayFormatter {
    private val priceFormatter = ThreadLocal.withInitial { DecimalFormat("#,###") }
    private val nutritionFormatter = ThreadLocal.withInitial { DecimalFormat("#.##") }

    fun formatPrice(value: Int): String = priceFormatter.get()!!.format(value)

    fun formatNutrition(value: Double): String = nutritionFormatter.get()!!.format(value)

    fun macroRatios(
        carbohydrate: Double,
        protein: Double,
        fat: Double
    ): Triple<Int, Int, Int> {
        val carbohydrateCalories = carbohydrate * 4
        val proteinCalories = protein * 4
        val fatCalories = fat * 9
        val totalCalories = carbohydrateCalories + proteinCalories + fatCalories
        if (totalCalories <= 0.0) return Triple(0, 0, 0)

        return Triple(
            (carbohydrateCalories / totalCalories * 100).roundToInt(),
            (proteinCalories / totalCalories * 100).roundToInt(),
            (fatCalories / totalCalories * 100).roundToInt()
        )
    }
}
