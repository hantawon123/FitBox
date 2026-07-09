package com.ssafy.fitbox.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ssafy.fitbox.dto.DietReport
import com.ssafy.fitbox.dto.FavoriteIngredient
import com.ssafy.fitbox.dto.FavoriteMeal
import com.ssafy.fitbox.network.response.OrderResponse
import com.ssafy.fitbox.network.response.SubscriptionTemplateResponse

object FavoriteMealStore {
    private const val PREF_NAME = "favorite_meals"
    private val gson = Gson()
    private var context: Context? = null

    fun initialize(context: Context) {
        this.context = context.applicationContext
    }

    fun getFavorites(userId: Int): List<FavoriteMeal> {
        val prefs = context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) ?: return emptyList()
        val json = prefs.getString(key(userId), null) ?: return emptyList()
        val type = object : TypeToken<List<FavoriteMeal>>() {}.type
        return runCatching { gson.fromJson<List<FavoriteMeal>>(json, type) }
            .getOrNull()
            .orEmpty()
            .sortedByDescending { it.createdAt }
    }

    fun addFromOrder(userId: Int, order: OrderResponse): Boolean {
        val mealId = order.mealId ?: return false
        val unitPrice = if (order.quantity > 0) order.totalPrice / order.quantity else order.totalPrice
        return addFromMeal(
            userId = userId,
            mealId = mealId,
            name = order.mealName.ifBlank { "주문 식단" },
            price = unitPrice
        )
    }

    fun addFromSubscriptionTemplate(
        userId: Int,
        template: SubscriptionTemplateResponse
    ): Boolean {
        return addFromMeal(
            userId = userId,
            mealId = template.mealId,
            name = template.mealName.ifBlank { "구독 식단" },
            price = template.mealPrice
        )
    }

    fun addFromMeal(
        userId: Int,
        mealId: Long,
        name: String,
        price: Int
    ): Boolean {
        if (getFavorites(userId).any { it.mealId == mealId }) {
            return false
        }

        val favorite = FavoriteMeal(
            id = "meal_$mealId",
            userId = userId,
            mealId = mealId,
            name = name,
            sourceType = FavoriteMeal.SOURCE_ORDER,
            calories = 0.0,
            carbohydrate = 0.0,
            protein = 0.0,
            fat = 0.0,
            price = price,
            description = "식단 상세 정보를 불러오는 중입니다."
        )
        putFavorite(userId, favorite)
        return true
    }

    fun addFromDietReport(userId: Int, report: DietReport, mealName: String): Boolean {
        if (report.items.isEmpty()) return false
        val signature = report.items.joinToString("|") { "${it.name}:${it.amount}" }
        val favorite = FavoriteMeal(
            id = "ai_${signature.hashCode()}",
            userId = userId,
            mealId = null,
            name = mealName.trim(),
            sourceType = FavoriteMeal.SOURCE_AI,
            calories = report.totalCalories,
            carbohydrate = report.totalCarbs,
            protein = report.totalProtein,
            fat = report.totalFat,
            price = report.totalPrice,
            ingredients = report.items.map {
                FavoriteIngredient(name = it.name, amount = it.amount, calories = it.calories)
            }
        )
        putFavorite(userId, favorite)
        return true
    }

    fun removeFavorite(userId: Int, favoriteId: String) {
        saveFavorites(userId, getFavorites(userId).filterNot { it.id == favoriteId })
    }

    private fun putFavorite(userId: Int, favorite: FavoriteMeal) {
        val favorites = getFavorites(userId)
            .filterNot { it.id == favorite.id }
            .toMutableList()
        favorites.add(0, favorite.copy(createdAt = System.currentTimeMillis()))
        saveFavorites(userId, favorites)
    }

    private fun saveFavorites(userId: Int, favorites: List<FavoriteMeal>) {
        context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(key(userId), gson.toJson(favorites))
            ?.apply()
    }

    private fun key(userId: Int): String = "favorites_$userId"
}
