package com.ssafy.fitbox.util

import android.content.Context
import com.google.gson.Gson
import com.ssafy.fitbox.dto.DietReport
import com.ssafy.fitbox.dto.Ingredient
import com.ssafy.fitbox.dto.User
import java.security.MessageDigest

object DietReportCache {
    private const val PREF_NAME = "diet_report_cache"
    private const val CACHE_VERSION = "preference_priority_v5_ingredient_benefits"
    private val gson = Gson()
    private var context: Context? = null

    fun initialize(context: Context) {
        this.context = context.applicationContext
    }

    fun createKey(user: User, ingredients: List<Ingredient>, userPreference: String): String {
        val ingredientSignature = ingredients
            .sortedBy { it.id }
            .joinToString("|") {
                listOf(
                    it.id,
                    it.name,
                    it.calories,
                    it.carbohydrate,
                    it.protein,
                    it.fat,
                    it.price
                ).joinToString(":")
            }

        val rawKey = listOf(
            CACHE_VERSION,
            user.id,
            user.gender,
            user.age,
            user.height,
            user.weight,
            user.activityLevel,
            user.purpose,
            user.allergies.sorted().joinToString(","),
            userPreference,
            ingredientSignature
        ).joinToString("||")

        return "diet_report_${sha256(rawKey)}"
    }

    fun getReport(key: String): DietReport? {
        val prefs = context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) ?: return null
        val json = prefs.getString(key, null) ?: return null
        return runCatching { gson.fromJson(json, DietReport::class.java) }.getOrNull()
    }

    fun putReport(key: String, report: DietReport) {
        context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(key, gson.toJson(report))
            ?.apply()
    }

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
