package com.ssafy.fitbox.util

import android.content.Context
import com.ssafy.fitbox.dto.AdminOrder
import com.ssafy.fitbox.dto.AdminOrderStatus
import com.ssafy.fitbox.dto.AdminPickupMode
import com.ssafy.fitbox.network.response.OrderResponse

object AdminOrderStore {
    private const val PREF_NAME = "admin_order_state"

    fun merge(context: Context, orders: List<OrderResponse>): List<AdminOrder> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return orders
            .groupBy(::groupKey)
            .values
            .map { groupedOrders ->
                val representative = groupedOrders.minBy { it.orderId }
                val status = groupedOrders
                    .map { AdminOrderStatus.fromCode(it.orderStatus) }
                    .distinct()
                    .singleOrNull()
                    ?: AdminOrderStatus.fromCode(representative.orderStatus)

            AdminOrder(
                order = representative,
                orders = groupedOrders.sortedBy { it.orderId },
                status = status,
                pickupMode = when (representative.receiveType) {
                    "DELIVERY" -> AdminPickupMode.DELIVERY
                    "PICKUP_POINT" -> AdminPickupMode.PICKUP_POINT
                    else -> AdminPickupMode.STORE
                },
                pickupPoint = representative.pickupPointName
                    ?: prefs.getString(pointKey(representative.orderId), null),
                lastPushMessage = if (status == AdminOrderStatus.PICKED_UP) {
                    "사용자가 상품 픽업을 완료했습니다."
                } else {
                    prefs.getString(pushKey(representative.orderId), null)
                }
            )
            }
    }

    fun save(context: Context, adminOrder: AdminOrder) {
        val editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()

        adminOrder.orders.forEach { order ->
            editor
                .putString(pointKey(order.orderId), adminOrder.pickupPoint)
                .putString(pushKey(order.orderId), adminOrder.lastPushMessage)
        }

        editor
            .apply()
    }

    private fun groupKey(order: OrderResponse): String {
        return listOf(
            order.userId,
            order.orderTime,
            order.receiveType,
            order.receiveDate.orEmpty(),
            order.storeId?.toString().orEmpty(),
            order.pickupPointId?.toString().orEmpty(),
            order.address.orEmpty(),
            order.paymentStatus.orEmpty(),
            order.orderType
        ).joinToString("|")
    }

    private fun pointKey(id: Long) = "point_$id"
    private fun pushKey(id: Long) = "push_$id"

    private fun dummyOrders(): List<OrderResponse> {
        return listOf(
            dummyOrder(9001, 1, "고단백 닭가슴살 도시락", "PICKUP", "핏박스 강남점"),
            dummyOrder(9002, 2, "연어 아보카도 샐러드", "PICKUP_POINT", "핏박스 역삼점"),
            dummyOrder(9003, 3, "저탄수 두부 샐러드", "PICKUP", "핏박스 삼성점")
        )
    }

    private fun dummyOrder(
        orderId: Long,
        userId: Int,
        mealName: String,
        receiveType: String,
        storeName: String
    ) = OrderResponse(
        orderId = orderId,
        subscriptionGroupId = null,
        userId = userId,
        customerName = when (userId) {
            1 -> "김민준"
            2 -> "이서연"
            else -> "박지훈"
        },
        mealId = orderId,
        mealName = mealName,
        quantity = 1,
        totalPrice = 8900,
        receiveType = receiveType,
        receiveDate = "2026-06-23",
        dateStart = null,
        dateEnd = null,
        mon = null,
        tue = null,
        wed = null,
        thu = null,
        fri = null,
        sat = null,
        sun = null,
        storeId = 1,
        storeName = storeName,
        storeAddress = "서울시 강남구",
        address = null,
        paymentStatus = "SUCCESS",
        orderType = "SINGLE",
        subscriptionStatus = null,
        orderTime = "2026-06-23T09:00:00",
        subscriptionItemsText = null
    )
}
