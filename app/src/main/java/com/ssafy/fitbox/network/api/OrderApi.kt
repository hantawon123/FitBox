package com.ssafy.fitbox.network.api

import com.ssafy.fitbox.network.request.DirectOrderRequest
import com.ssafy.fitbox.network.request.OrderCartRequest
import com.ssafy.fitbox.network.request.OrderStatusUpdateRequest
import com.ssafy.fitbox.network.request.LockerAssignmentRequest
import com.ssafy.fitbox.network.request.NfcPickupRequest
import com.ssafy.fitbox.network.request.SelectedDateSubscriptionOrderRequest
import com.ssafy.fitbox.network.request.SubscriptionCreateRequest
import com.ssafy.fitbox.network.request.SubscriptionOrderRequest
import com.ssafy.fitbox.network.response.OrderResponse
import com.ssafy.fitbox.network.response.SubscriptionOrderResponse
import com.ssafy.fitbox.network.response.SubscriptionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.PATCH
import retrofit2.http.Query
import okhttp3.ResponseBody

interface OrderApi {

    @POST("orderapi/orders/direct")
    suspend fun orderDirect(
        @Body request: DirectOrderRequest
    ): Response<OrderResponse>

    @POST("orderapi/orders/cart")
    suspend fun orderFromCart(
        @Body request: OrderCartRequest
    ): Response<List<OrderResponse>>

    @POST("subscriptorderapi/order")
    suspend fun orderSubscription(
        @Body request: SubscriptionOrderRequest
    ): Response<SubscriptionOrderResponse>

    @POST("subscriptorderapi/order/selected-dates")
    suspend fun orderSelectedDateSubscription(
        @Body request: SelectedDateSubscriptionOrderRequest
    ): Response<List<SubscriptionOrderResponse>>

    @POST("subscriptorderapi/subscriptions")
    suspend fun createMonthlySubscription(
        @Body request: SubscriptionCreateRequest
    ): Response<SubscriptionResponse>

    @GET("subscriptorderapi/subscriptions/users/{userId}")
    suspend fun getUserSubscriptions(
        @Path("userId") userId: Int
    ): Response<List<SubscriptionResponse>>

    @PATCH("subscriptorderapi/subscriptions/{subscriptionGroupId}/cancel")
    suspend fun cancelSubscription(
        @Path("subscriptionGroupId") subscriptionGroupId: Long,
        @Query("userId") userId: Int
    ): Response<ResponseBody>

    @GET("orderapi/orders/users/{userId}")
    suspend fun getUserOrders(
        @Path("userId") userId: Long
    ): Response<List<OrderResponse>>

    @GET("orderapi/admin/orders")
    suspend fun getAdminOrders(): Response<List<OrderResponse>>

    @PATCH("orderapi/orders/{orderId}/status")
    suspend fun updateOrderStatus(
        @Path("orderId") orderId: Long,
        @Body request: OrderStatusUpdateRequest
    ): Response<OrderResponse>

    @PATCH("orderapi/orders/{orderId}/locker")
    suspend fun assignLocker(
        @Path("orderId") orderId: Long,
        @Body request: LockerAssignmentRequest
    ): Response<OrderResponse>

    @POST("orderapi/orders/{orderId}/nfc-pickup")
    suspend fun completeNfcPickup(
        @Path("orderId") orderId: Long,
        @Body request: NfcPickupRequest
    ): Response<OrderResponse>
}
