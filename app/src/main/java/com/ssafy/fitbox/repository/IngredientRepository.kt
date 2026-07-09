package com.ssafy.fitbox.repository

import android.util.Log
import com.ssafy.fitbox.dto.Ingredient
import com.ssafy.fitbox.network.RetrofitClient
import com.ssafy.fitbox.network.api.IngredientApi
import java.io.IOException

class IngredientRepository(
    private val ingredientApi: IngredientApi = RetrofitClient.ingredientApi
) {
    suspend fun getIngredients(): Result<List<Ingredient>> {
        return try {
            val response = ingredientApi.getIngredients()

            if (response.isSuccessful) {
                val ingredients = response.body()
                    ?.map { it.toIngredient() }
                    ?: emptyList()

                Log.d(TAG, "재료 목록 조회 성공: ${ingredients.size}개")

                Result.success(ingredients)
            } else {
                val errorMessage = response.errorBody()?.string()
                Log.e(TAG, "재료 목록 조회 실패: code=${response.code()}, error=$errorMessage")

                Result.failure(
                    Exception("재료 목록 조회 실패: code=${response.code()}")
                )
            }
        } catch (e: IOException) {
            Log.e(TAG, "네트워크 연결 실패", e)
            Result.failure(Exception("서버에 연결할 수 없습니다. 네트워크 상태를 확인해주세요."))
        } catch (e: Exception) {
            Log.e(TAG, "알 수 없는 오류 발생", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "IngredientRepository"
    }
}