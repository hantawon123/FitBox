package com.ssafy.fitbox.network.request

data class SubscriptionCreateRequest(
    val userId: Int,
    val subscriptionStartDate: String,
    val receiveType: String,
    val storeId: Long?,
    val address: String?,
    val templates: List<SubscriptionTemplateRequest>
)