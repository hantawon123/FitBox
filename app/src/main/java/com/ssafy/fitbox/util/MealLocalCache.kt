package com.ssafy.fitbox.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ssafy.fitbox.dto.Product

object MealLocalCache {
    private const val PREF_NAME = "meal_local_cache"
    private const val CACHE_DURATION_MS = 6 * 60 * 60 * 1000L
    private val gson = Gson()
    private var context: Context? = null

    fun initialize(context: Context) {
        this.context = context.applicationContext
    }

    fun getProducts(key: String): List<Product>? {
        val prefs = context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) ?: return null
        val savedAt = prefs.getLong("${key}_saved_at", 0L)
        if (System.currentTimeMillis() - savedAt > CACHE_DURATION_MS) return null
        val json = prefs.getString(key, null) ?: return null
        val type = object : TypeToken<List<Product>>() {}.type
        return runCatching { gson.fromJson<List<Product>>(json, type) }.getOrNull()
    }

    fun putProducts(key: String, products: List<Product>) {
        context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(key, gson.toJson(products))
            ?.putLong("${key}_saved_at", System.currentTimeMillis())
            ?.apply()
    }

    fun getProduct(key: String): Product? {
        val prefs = context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) ?: return null
        val savedAt = prefs.getLong("${key}_saved_at", 0L)
        if (System.currentTimeMillis() - savedAt > CACHE_DURATION_MS) return null
        return prefs.getString(key, null)?.let {
            runCatching { gson.fromJson(it, Product::class.java) }.getOrNull()
        }
    }

    fun putProduct(key: String, product: Product) {
        context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(key, gson.toJson(product))
            ?.putLong("${key}_saved_at", System.currentTimeMillis())
            ?.apply()
    }
}
