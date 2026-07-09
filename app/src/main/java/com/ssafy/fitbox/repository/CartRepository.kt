package com.ssafy.fitbox.repository

import android.util.Log
import com.ssafy.fitbox.dto.CartItem
import com.ssafy.fitbox.network.RetrofitClient
import com.ssafy.fitbox.network.api.CartApi
import com.ssafy.fitbox.network.request.CartItemRequest
import java.io.IOException

class CartRepository(
    private val cartApi: CartApi = RetrofitClient.cartApi
) {
    suspend fun getCartItems(userId: Int): Result<List<CartItem>> {
        return try {
            val response = cartApi.getCart(userId)

            if (response.isSuccessful) {
                Result.success(response.body()?.toCartItems() ?: emptyList())
            } else {
                Result.failure(Exception("장바구니 조회 실패: code=${response.code()}"))
            }
        } catch (e: IOException) {
            Log.e(TAG, "장바구니 조회 네트워크 실패", e)
            Result.failure(Exception("서버에 연결할 수 없습니다."))
        } catch (e: Exception) {
            Log.e(TAG, "장바구니 조회 실패", e)
            Result.failure(e)
        }
    }

    suspend fun addCartItem(
        userId: Int,
        request: CartItemRequest
    ): Result<List<CartItem>> {
        return try {
            val response = cartApi.addCartItem(userId, request)

            if (response.isSuccessful) {
                Result.success(response.body()?.toCartItems() ?: emptyList())
            } else {
                Result.failure(Exception("장바구니 담기 실패: code=${response.code()}"))
            }
        } catch (e: IOException) {
            Log.e(TAG, "장바구니 담기 네트워크 실패", e)
            Result.failure(Exception("서버에 연결할 수 없습니다."))
        } catch (e: Exception) {
            Log.e(TAG, "장바구니 담기 실패", e)
            Result.failure(e)
        }
    }

    suspend fun deleteCartItem(cartItemId: Long): Result<Unit> {
        return try {
            val response = cartApi.deleteCartItem(cartItemId)

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("장바구니 아이템 삭제 실패: code=${response.code()}"))
            }
        } catch (e: IOException) {
            Log.e(TAG, "장바구니 삭제 네트워크 실패", e)
            Result.failure(Exception("서버에 연결할 수 없습니다."))
        } catch (e: Exception) {
            Log.e(TAG, "장바구니 삭제 실패", e)
            Result.failure(e)
        }
    }

    suspend fun clearCart(userId: Int): Result<Unit> {
        return try {
            val response = cartApi.clearCart(userId)

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("장바구니 비우기 실패: code=${response.code()}"))
            }
        } catch (e: IOException) {
            Log.e(TAG, "장바구니 비우기 네트워크 실패", e)
            Result.failure(Exception("서버에 연결할 수 없습니다."))
        } catch (e: Exception) {
            Log.e(TAG, "장바구니 비우기 실패", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "CartRepository"
    }
}