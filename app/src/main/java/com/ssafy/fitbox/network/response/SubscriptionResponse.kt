package com.ssafy.fitbox.network.response

data class SubscriptionResponse(
    val subscriptionGroupId: Long,
    val userId: Int,
    val orderTime: String?,
    val subscriptionStartDate: String,
    val subscriptionEndDate: String?,
    val nextOrderMonth: String?,
    val status: String,
    val receiveType: String,
    val storeId: Long?,
    val storeName: String?,
    val storeAddress: String?,
    val address: String?,
    val templates: List<SubscriptionTemplateResponse>?
)