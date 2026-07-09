package com.ssafy.fitbox.network.response

data class SubscriptionTemplateResponse(
    val templateId: Long,
    val weekOfMonth: Int,
    val dayOfWeek: Int,
    val dayOfWeekText: String,
    val mealId: Long,
    val mealName: String,
    val mealPrice: Int,
    val quantity: Int
)
