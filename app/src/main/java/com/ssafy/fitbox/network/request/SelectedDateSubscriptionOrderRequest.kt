package com.ssafy.fitbox.network.request

data class SelectedDateSubscriptionOrderRequest(
    val userId: Int,
    val mealId: Long,
    val receiveDates: List<String>,
    val receiveType: String,
    val storeId: Long?,
    val address: String?
)