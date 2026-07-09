package com.ssafy.fitbox.network.api

import com.ssafy.fitbox.network.request.CartItemRequest
import com.ssafy.fitbox.network.response.CartResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CartApi {

    @GET("carts/{userId}")
    suspend fun getCart(
        @Path("userId") userId: Int
    ): Response<CartResponse>

    @POST("carts/{userId}/items")
    suspend fun addCartItem(
        @Path("userId") userId: Int,
        @Body request: CartItemRequest
    ): Response<CartResponse>

    @DELETE("carts/items/{cartItemId}")
    suspend fun deleteCartItem(
        @Path("cartItemId") cartItemId: Long
    ): Response<Unit>

    @DELETE("carts/{userId}")
    suspend fun clearCart(
        @Path("userId") userId: Int
    ): Response<Unit>
}