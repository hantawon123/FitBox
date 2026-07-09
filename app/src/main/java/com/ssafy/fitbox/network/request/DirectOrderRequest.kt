package com.ssafy.fitbox.network.request

data class DirectOrderRequest(
    val userId: Int,
    val mealId: Long,
    val quantity: Int = 1,
    val receiveType: String,
    val storeId: Long?,
    val pickupPointId: Long? = null,
    val address: String?,
    val receiveDate: String?,
    val paymentMethod: String = "MOCK"
)
