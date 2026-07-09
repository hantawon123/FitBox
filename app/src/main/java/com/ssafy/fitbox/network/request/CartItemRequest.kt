package com.ssafy.fitbox.network.request

data class CartItemRequest(
    val mealId: Long,
    val quantity: Int = 1
)