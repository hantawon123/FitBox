package com.ssafy.fitbox.dto

import com.ssafy.fitbox.network.response.OrderResponse

enum class AdminOrderStatus(val displayName: String) {
    ORDER_REVIEW("주문 검토 중"),
    PREPARING("준비중"),
    READY("준비완료"),
    PICKED_UP("픽업완료");

    companion object {
        fun fromCode(code: String?): AdminOrderStatus {
            return values().firstOrNull { it.name == code } ?: ORDER_REVIEW
        }
    }
}

enum class AdminPickupMode(val displayName: String) {
    STORE("매장 픽업"),
    DELIVERY("배달 주문"),
    PICKUP_POINT("픽업 포인트")
}

data class AdminOrder(
    val order: OrderResponse,
    val orders: List<OrderResponse> = listOf(order),
    val status: AdminOrderStatus,
    val pickupMode: AdminPickupMode,
    val pickupPoint: String? = null,
    val lastPushMessage: String? = null
)
