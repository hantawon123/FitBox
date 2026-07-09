package com.ssafy.fitbox.dto

import com.ssafy.fitbox.network.response.OrderResponse

data class OrderHistoryItem(
    val orders: List<OrderResponse>
) {
    val representative: OrderResponse = orders.first()
    val isGrouped: Boolean = orders.size > 1
    val totalQuantity: Int = orders.sumOf { it.quantity }
    val totalPrice: Int = orders.sumOf { it.totalPrice }
}
