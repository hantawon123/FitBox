package com.ssafy.fitbox.notification

import com.ssafy.fitbox.repository.NotificationRepository

interface PushNotificationSender {
    suspend fun send(userId: Int, title: String, message: String): Result<Unit>
}

class FirebasePushNotificationSender(
    private val repository: NotificationRepository = NotificationRepository()
) : PushNotificationSender {
    override suspend fun send(
        userId: Int,
        title: String,
        message: String
    ): Result<Unit> {
        return repository.send(userId, title, message)
    }
}
