package com.ssafy.fitbox.network.request

data class OrderCartRequest(
    val userId: Int,
    val receiveType: String,
    val storeId: Long?,
    val pickupPointId: Long? = null,
    val address: String?,
    val receiveDate: String?,
    val paymentMethod: String = "MOCK"
)
