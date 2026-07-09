package com.ssafy.fitbox.dto

import com.ssafy.fitbox.util.AddressParts

data class Address(
    val id: Int = 0,
    val userId: Int,
    val address: String = "",
    val zoneCode: String = "",
    val roadAddress: String = "",
    val detailAddress: String = ""
) {
    val displayAddress: String
        get() = address.ifBlank {
            AddressParts.compose(
                zoneCode = zoneCode,
                roadAddress = roadAddress,
                detailAddress = detailAddress
            )
        }
}
