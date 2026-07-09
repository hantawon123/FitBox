package com.ssafy.fitbox.network.api

import com.ssafy.fitbox.dto.User
import com.ssafy.fitbox.dto.UserPreference
import com.ssafy.fitbox.network.request.FindUserIdRequest
import com.ssafy.fitbox.network.request.KakaoLoginRequest
import com.ssafy.fitbox.network.request.ResetPasswordRequest
import com.ssafy.fitbox.network.response.FindUserIdResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface UserApi {
    @POST("/users")
    suspend fun createUser(@Body user: User): Response<ResponseBody>

    @GET("/users/check/id/{userId}")
    suspend fun checkId(@Path("userId") userId: String): Response<Boolean>

    @GET("/users/check/phone/{phone}")
    suspend fun checkPhone(@Path("phone") phone: String): Response<Boolean>

    @POST("/users/login")
    suspend fun login(@Body user: User): Response<User>

    @POST("/users/login/kakao")
    suspend fun loginWithKakao(
        @Body request: KakaoLoginRequest
    ): Response<User>

    @POST("/users/find-id")
    suspend fun findUserId(
        @Body request: FindUserIdRequest
    ): Response<FindUserIdResponse>

    @POST("/users/reset-password")
    suspend fun resetPassword(
        @Body request: ResetPasswordRequest
    ): Response<ResponseBody>

    @GET("/users/user-id/{userId}")
    suspend fun getUserByUserId(@Path("userId") userId: String): Response<User>

    @PUT("/users/{id}")
    suspend fun updateUser(@Path("id") id: Int, @Body user: User): Response<ResponseBody>

    @POST("/users/{userId}/allergies")
    suspend fun updateUserAllergies(
        @Path("userId") userId: Int,
        @Body allergies: List<String>
    ): Response<ResponseBody>

    @GET("/preferences/{userId}")
    suspend fun getUserPreference(@Path("userId") userId: Int): Response<UserPreference>

    @POST("/preferences")
    suspend fun saveUserPreference(@Body preference: UserPreference): Response<ResponseBody>
}
