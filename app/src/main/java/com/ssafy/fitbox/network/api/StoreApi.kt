package com.ssafy.fitbox.network.api

import com.ssafy.fitbox.dto.Store
import com.ssafy.fitbox.dto.PickupPoint
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface StoreApi {

    @GET("storeapi/store")
    suspend fun getStores(
        @Query("pickupDate") pickupDate: String
    ): Response<List<Store>>

    @GET("storeapi/store/subscription")
    suspend fun getStoresForSubscription(
        @Query("dateStart") dateStart: String,
        @Query("dateEnd") dateEnd: String,
        @Query("mon") mon: Boolean,
        @Query("tue") tue: Boolean,
        @Query("wed") wed: Boolean,
        @Query("thu") thu: Boolean,
        @Query("fri") fri: Boolean,
        @Query("sat") sat: Boolean,
        @Query("sun") sun: Boolean
    ): Response<List<Store>>

    @GET("storeapi/pickup-points")
    suspend fun getPickupPoints(
        @Query("storeId") storeId: Long,
        @Query("pickupDate") pickupDate: String? = null
    ): Response<List<PickupPoint>>

    @GET("storeapi/pickup-points/bounds")
    suspend fun getPickupPointsInBounds(
        @Query("south") south: Double,
        @Query("north") north: Double,
        @Query("west") west: Double,
        @Query("east") east: Double,
        @Query("pickupDate") pickupDate: String? = null
    ): Response<List<PickupPoint>>

    @GET("storeapi/pickup-points/{id}/capacity")
    suspend fun getPickupPointCapacity(
        @retrofit2.http.Path("id") pickupPointId: Long,
        @Query("pickupDate") pickupDate: String
    ): Response<PickupPoint>
}
