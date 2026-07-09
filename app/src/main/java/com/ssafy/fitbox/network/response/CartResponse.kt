package com.ssafy.fitbox.network.response

import CartItemResponse
import com.ssafy.fitbox.dto.CartItem
import kotlin.collections.map

data class CartResponse(
    val cartId: Long,
    val userId: Int,
    val items: List<CartItemResponse>,
    val totalPrice: Int
) {
    fun toCartItems(): List<CartItem> {
        return items.map { it.toCartItem() }
    }
}