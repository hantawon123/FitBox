package com.ssafy.fitbox.network.api

import com.ssafy.fitbox.dto.AppNotification
import com.ssafy.fitbox.dto.FcmTokenRequest
import com.ssafy.fitbox.dto.PushNotificationRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface NotificationApi {
    @POST("notificationapi/tokens")
    suspend fun registerToken(@Body request: FcmTokenRequest): Response<ResponseBody>

    @POST("notificationapi/send")
    suspend fun sendNotification(
        @Body request: PushNotificationRequest
    ): Response<ResponseBody>

    @GET("notificationapi/users/{userId}")
    suspend fun getNotifications(
        @Path("userId") userId: Int
    ): Response<List<AppNotification>>

    @GET("notificationapi/users/{userId}/unread-count")
    suspend fun getUnreadCount(
        @Path("userId") userId: Int
    ): Response<Int>

    @PUT("notificationapi/users/{userId}/read-all")
    suspend fun markAllAsRead(
        @Path("userId") userId: Int
    ): Response<Unit>
}
