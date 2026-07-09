package com.ssafy.fitbox.network.request

data class SubscriptionTemplateRequest(
    val weekOfMonth: Int,
    val dayOfWeek: Int,
    val mealId: Long,
    val quantity: Int
)