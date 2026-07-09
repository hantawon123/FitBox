package com.ssafy.fitbox.notification

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.ssafy.fitbox.repository.NotificationRepository
import com.ssafy.fitbox.util.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object FcmTokenRegistrar {
    private const val TAG = "FcmTokenRegistrar"

    private const val PREFS_NAME = "FitBoxFcmPrefs"
    private const val KEY_TOKEN = "fcm_token"
    private const val KEY_REGISTERED_USER_ID = "registered_user_id"

    fun registerCurrentUser(context: Context) {
        val appContext = context.applicationContext
        val userId = SessionManager(appContext).getUser()?.id

        if (userId == null || userId <= 0) {
            Log.d(TAG, "로그인 사용자가 없어 FCM 토큰 등록을 건너뜁니다.")
            return
        }

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (token.isNullOrBlank()) {
                    Log.e(TAG, "FCM 토큰이 비어 있습니다.")
                    return@addOnSuccessListener
                }

                saveToken(appContext, token)
                register(appContext, userId, token)
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "FCM 토큰 조회 실패", error)

                val savedToken = preferences(appContext).getString(KEY_TOKEN, null)
                if (!savedToken.isNullOrBlank()) {
                    register(appContext, userId, savedToken)
                }
            }
    }

    fun register(context: Context, userId: Int, token: String) {
        val appContext = context.applicationContext

        if (userId <= 0) {
            Log.d(TAG, "Skip FCM token registration before user is saved. userId=$userId")
            return
        }

        if (token.isBlank()) {
            Log.e(TAG, "빈 FCM 토큰은 서버에 등록하지 않습니다.")
            return
        }

        saveToken(appContext, token)

        CoroutineScope(Dispatchers.IO).launch {
            NotificationRepository().registerToken(userId, token)
                .onSuccess {
                    preferences(appContext)
                        .edit()
                        .putInt(KEY_REGISTERED_USER_ID, userId)
                        .apply()

                    Log.i(TAG, "FCM 토큰 서버 등록 성공: userId=$userId")
                }
                .onFailure { error ->
                    Log.e(TAG, "FCM 토큰 서버 등록 실패: userId=$userId", error)
                }
        }
    }

    fun clearLocalRegistration(context: Context) {
        preferences(context.applicationContext)
            .edit()
            .remove(KEY_REGISTERED_USER_ID)
            .apply()
    }

    private fun saveToken(context: Context, token: String) {
        preferences(context)
            .edit()
            .putString(KEY_TOKEN, token)
            .apply()
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
