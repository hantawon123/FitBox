package com.ssafy.fitbox.network.response

data class SubscriptionOrderResponse(
    val id: Long?,
    val userId: Int,
    val orderTime: String?,
    val dateStart: String,
    val dateEnd: String,
    val mon: Long?,
    val tue: Long?,
    val wed: Long?,
    val thu: Long?,
    val fri: Long?,
    val sat: Long?,
    val sun: Long?,
    val receiveType: String,
    val storeId: Long?,
    val address: String?
)