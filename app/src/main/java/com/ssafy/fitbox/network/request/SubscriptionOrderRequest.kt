package com.ssafy.fitbox.network.request

data class SubscriptionOrderRequest(
    val userId: Int,
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