package com.ssafy.fitbox.dto

data class SubscriptionPlanItem(
    val weekOfMonth: Int,
    val dayOfWeek: Int,
    val dayOfWeekText: String,
    val product: Product,
    val quantity: Int
)