package com.ssafy.fitbox.util

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson // 🌟 객체 변환을 위해 추가
import com.ssafy.fitbox.dto.User // 🌟 User DTO 추가

class SessionManager(context: Context) {
    // FitBoxPrefs 라는 이름의 내부 저장소 파일 생성
    private val prefs: SharedPreferences = context.getSharedPreferences("FitBoxPrefs", Context.MODE_PRIVATE)
    private val editor = prefs.edit()
    private val gson = Gson() // 🌟 추가

    companion object {
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_USER_INFO = "userInfo" // 🌟 유저 객체를 통째로 저장할 키
    }

    // 🌟 [수정된 부분] 로그인 성공 시 User 객체 전체를 세션에 저장
    fun createSession(user: User) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putString(KEY_USER_INFO, gson.toJson(user)) // User 객체를 JSON 문자열로 변환하여 저장
        editor.apply() // 비동기로 저장
    }

    // 로그인 여부 확인
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    // 🌟 [추가된 부분] 현재 로그인된 유저의 전체 정보 가져오기
    fun getUser(): User? {
        val json = prefs.getString(KEY_USER_INFO, null)
        return if (json != null) {
            gson.fromJson(json, User::class.java) // 저장된 문자열을 다시 User 객체로 복원
        } else {
            null
        }
    }

    // 로그아웃 시 세션 정보 싹 지우기
    fun clearSession() {
        editor.clear()
        editor.apply()
    }
}