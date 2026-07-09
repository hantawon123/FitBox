package com.ssafy.fitbox.notification

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ssafy.fitbox.R
import com.ssafy.fitbox.activity.MainActivity
import com.ssafy.fitbox.util.SessionManager

class FitBoxMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        val userId = SessionManager(applicationContext).getUser()?.id

        if (userId == null) {
            Log.d(TAG, "로그인 사용자가 없어 새 FCM 토큰 서버 등록을 건너뜁니다.")
            return
        }

        FcmTokenRegistrar.register(applicationContext, userId, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "FitBox 알림"

        val body = message.notification?.body
            ?: message.data["message"]
            ?: message.data["body"]
            ?: message.data["content"]
            ?: return

        NotificationEvents.notifyArrived()
        showSystemNotification(title, body)
    }

    private fun showSystemNotification(title: String, body: String) {
        if (!hasNotificationPermission()) {
            Log.d(TAG, "알림 권한이 없어 시스템 알림을 표시하지 않습니다.")
            return
        }

        FitBoxNotificationChannel.create(this)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_NOTIFICATIONS, true)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            this,
            FitBoxNotificationChannel.CHANNEL_ID
        )
            .setSmallIcon(R.drawable.notifications)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val TAG = "FitBoxMessagingService"
        const val EXTRA_OPEN_NOTIFICATIONS = "open_notifications"
    }
}
