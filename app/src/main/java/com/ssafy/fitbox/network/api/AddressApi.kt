package com.ssafy.fitbox.network.api

import com.ssafy.fitbox.dto.Address
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AddressApi {
    @GET("addresses/user/{userId}")
    suspend fun getUserAddresses(
        @Path("userId") userId: Int
    ): Response<List<Address>>

    @POST("addresses")
    suspend fun createAddress(
        @Body address: Address
    ): Response<Address>

    @DELETE("addresses/{id}")
    suspend fun deleteAddress(
        @Path("id") id: Int
    ): Response<okhttp3.ResponseBody>
}
