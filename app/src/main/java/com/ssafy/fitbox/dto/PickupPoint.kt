package com.ssafy.fitbox.dto

data class PickupPoint(
    val id: Long,
    val storeId: Long,
    val name: String,
    val address: String,
    val longitude: Double,
    val latitude: Double,
    val totalCnt: Int = 0,
    val remainCnt: Int = 0
)
