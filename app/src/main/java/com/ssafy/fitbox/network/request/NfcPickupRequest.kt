package com.ssafy.fitbox.network.request

data class NfcPickupRequest(
    val userId: Int,
    val pickupPointName: String,
    val lockerNumber: String
)
