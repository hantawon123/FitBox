package com.ssafy.fitbox.network.api

import com.ssafy.fitbox.network.response.IngredientResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface IngredientApi {

    @GET("ingredients")
    suspend fun getIngredients(): Response<List<IngredientResponse>>

    @GET("ingredients/{id}")
    suspend fun getIngredientById(
        @Path("id") ingredientId: Int
    ): Response<IngredientResponse>
}