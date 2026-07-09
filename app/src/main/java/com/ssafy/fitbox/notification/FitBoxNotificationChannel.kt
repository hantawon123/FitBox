package com.ssafy.fitbox.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object FitBoxNotificationChannel {
    const val CHANNEL_ID = "fitbox_order_notifications_v2"

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "FitBox 주문 알림",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "신규 주문 및 주문 상태 변경 알림"
            enableVibration(true)
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
    }
}
