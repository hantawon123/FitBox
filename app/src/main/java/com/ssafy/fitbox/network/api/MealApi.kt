package com.ssafy.fitbox.network.api

import com.ssafy.fitbox.network.request.CustomMealCreateRequest
import com.ssafy.fitbox.network.response.CustomMealCreateResponse
import com.ssafy.fitbox.network.response.ProductResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface MealApi {

    @GET("meals/products")
    suspend fun getProductMeals(): Response<List<ProductResponse>>

    @GET("meals/{id}")
    suspend fun getMealById(
        @Path("id") mealId: Long
    ): Response<ProductResponse>

    @GET("meals/recommend/today")
    suspend fun getTodayRecommendedProduct(): Response<ProductResponse>

    @GET("meals/popular")
    suspend fun getPopularMeals(): Response<List<ProductResponse>>

    @GET("meals/popular/monthly")
    suspend fun getMonthlyPopularMeals(): Response<List<ProductResponse>>

    @GET("meals/popular/purpose/{userId}")
    suspend fun getPopularMealsBySamePurpose(
        @Path("userId") userId: Int
    ): Response<List<ProductResponse>>

    @POST("meals/custom")
    suspend fun createCustomMeal(
        @Body request: CustomMealCreateRequest
    ): Response<CustomMealCreateResponse>
}
