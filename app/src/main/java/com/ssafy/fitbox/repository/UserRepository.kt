package com.ssafy.fitbox.repository

import android.util.Log
import com.ssafy.fitbox.dto.User
import com.ssafy.fitbox.dto.UserPreference
import com.ssafy.fitbox.network.RetrofitClient
import com.ssafy.fitbox.network.api.UserApi
import com.ssafy.fitbox.network.request.FindUserIdRequest
import com.ssafy.fitbox.network.request.KakaoLoginRequest
import com.ssafy.fitbox.network.request.ResetPasswordRequest
import java.io.IOException

class UserRepository(
    private val userApi: UserApi = RetrofitClient.userApi
) {
    private val TAG = "UserRepository"

    // 아이디 중복 체크
    suspend fun checkId(userId: String): Result<Boolean> {
        return try {
            val response = userApi.checkId(userId)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception("아이디 중복 체크 실패"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "checkId 통신 에러", e)
            Result.failure(e)
        }
    }

    // 전화번호 중복 체크
    suspend fun checkPhone(phone: String): Result<Boolean> {
        return try {
            val response = userApi.checkPhone(phone)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception("전화번호 중복 체크 실패"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "checkPhone 통신 에러", e)
            Result.failure(e)
        }
    }

    // 회원가입
    suspend fun register(user: User): Result<String> {
        return try {
            val response = userApi.createUser(user)
            if (response.isSuccessful) {
                Result.success(response.body()?.string() ?: "가입 성공")
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "가입 실패"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "register 통신 에러", e)
            Result.failure(e)
        }
    }

    // 일반 로그인
    suspend fun login(user: User): Result<User> {
        return try {
            val response = userApi.login(user)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception("아이디 또는 비밀번호가 잘못되었습니다."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "login 통신 에러", e)
            Result.failure(e)
        }
    }

    // 카카오 로그인
    suspend fun loginWithKakao(accessToken: String): Result<User> {
        return try {
            val response = userApi.loginWithKakao(
                KakaoLoginRequest(accessToken = accessToken)
            )

            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                val errorMessage = response.errorBody()?.string()
                    ?: "카카오 로그인 실패: ${response.code()}"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "loginWithKakao 통신 에러", e)
            Result.failure(e)
        }
    }

    // 회원 정보 수정
    suspend fun findUserId(name: String, phone: String): Result<String> {
        return try {
            val response = userApi.findUserId(
                FindUserIdRequest(
                    name = name,
                    phone = phone
                )
            )
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body.userId)
            } else {
                Result.failure(Exception("입력한 정보와 일치하는 아이디를 찾을 수 없습니다."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "findUserId 통신 오류", e)
            Result.failure(e)
        }
    }

    suspend fun resetPassword(
        userId: String,
        name: String,
        phone: String,
        newPassword: String
    ): Result<String> {
        return try {
            val response = userApi.resetPassword(
                ResetPasswordRequest(
                    userId = userId,
                    name = name,
                    phone = phone,
                    newPassword = newPassword
                )
            )
            if (response.isSuccessful) {
                Result.success(response.body()?.string() ?: "비밀번호가 변경되었습니다.")
            } else {
                Result.failure(Exception("입력한 정보와 일치하는 회원을 찾을 수 없습니다."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "resetPassword 통신 오류", e)
            Result.failure(e)
        }
    }

    suspend fun updateUser(user: User): Result<String> {
        return try {
            val response = userApi.updateUser(user.id, user)

            if (response.isSuccessful) {
                Result.success(response.body()?.string() ?: "수정 성공")
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "수정 실패"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateUser 통신 에러", e)
            Result.failure(e)
        }
    }

    suspend fun getUserByUserId(userId: String): Result<User> {
        return try {
            val response = userApi.getUserByUserId(userId)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception("사용자 정보를 찾을 수 없습니다."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getUserByUserId 통신 에러", e)
            Result.failure(e)
        }
    }

    suspend fun getUserPreference(userId: Int): Result<String> {
        return try {
            val response = userApi.getUserPreference(userId)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body.preferencePrompt)
            } else {
                Result.success("")
            }
        } catch (e: Exception) {
            Result.success("")
        }
    }

    suspend fun saveUserPreference(userId: Int, prompt: String): Result<Boolean> {
        return try {
            val response = userApi.saveUserPreference(
                UserPreference(
                    userId = userId,
                    preferencePrompt = prompt
                )
            )
            if (response.isSuccessful) Result.success(true) else Result.failure(Exception("저장 실패"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserAllergies(userId: Int, allergies: List<String>): Result<Boolean> {
        return try {
            val response = userApi.updateUserAllergies(userId, allergies)
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(IOException("서버 알레르기 갱신 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
