package com.ssafy.fitbox.repository

import com.ssafy.fitbox.dto.AppNotification
import com.ssafy.fitbox.dto.FcmTokenRequest
import com.ssafy.fitbox.dto.PushNotificationRequest
import com.ssafy.fitbox.network.RetrofitClient
import com.ssafy.fitbox.network.api.NotificationApi

class NotificationRepository(
    private val notificationApi: NotificationApi = RetrofitClient.notificationApi
) {
    suspend fun registerToken(userId: Int, token: String): Result<Unit> = runCatching {
        val response = notificationApi.registerToken(FcmTokenRequest(userId, token))
        if (!response.isSuccessful) {
            error(response.errorBody()?.string() ?: "FCM 토큰 등록 실패")
        }
    }

    suspend fun send(userId: Int, title: String, message: String): Result<Unit> = runCatching {
        val response = notificationApi.sendNotification(
            PushNotificationRequest(userId, title, message)
        )
        if (!response.isSuccessful) {
            error(response.errorBody()?.string() ?: "FCM 알림 발송 실패")
        }
    }

    suspend fun getNotifications(userId: Int): Result<List<AppNotification>> = runCatching {
        val response = notificationApi.getNotifications(userId)
        if (!response.isSuccessful) {
            error(response.errorBody()?.string() ?: "알림 조회 실패")
        }
        response.body().orEmpty()
    }

    suspend fun getUnreadCount(userId: Int): Result<Int> = runCatching {
        val response = notificationApi.getUnreadCount(userId)
        if (!response.isSuccessful) {
            error(response.errorBody()?.string() ?: "미확인 알림 수 조회 실패")
        }
        response.body() ?: 0
    }

    suspend fun markAllAsRead(userId: Int): Result<Unit> = runCatching {
        val response = notificationApi.markAllAsRead(userId)
        if (!response.isSuccessful) {
            error(response.errorBody()?.string() ?: "알림 읽음 처리 실패")
        }
    }
}
