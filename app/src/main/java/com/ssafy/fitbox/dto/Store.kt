package com.ssafy.fitbox.dto

data class Store(
    val id: Long,
    val name: String,
    val address: String,
    val longitude: Double,
    val latitude: Double,
    val totalCnt: Int,
    val remainCnt: Int
)