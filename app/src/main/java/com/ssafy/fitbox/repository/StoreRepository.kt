package com.ssafy.fitbox.repository

import android.util.Log
import com.ssafy.fitbox.dto.PickupPoint
import com.ssafy.fitbox.dto.Store
import com.ssafy.fitbox.network.RetrofitClient
import com.ssafy.fitbox.network.api.StoreApi
import java.io.IOException

class StoreRepository(
    private val storeApi: StoreApi = RetrofitClient.storeApi
) {

    suspend fun getStores(
        pickupDate: String
    ): Result<List<Store>> {
        return try {
            val response = storeApi.getStores(pickupDate)

            if (response.isSuccessful) {
                Result.success(response.body().orEmpty())
            } else {
                Result.failure(Exception("매장 목록 조회 실패: code=${response.code()}"))
            }
        } catch (e: IOException) {
            Log.e(TAG, "매장 목록 네트워크 실패", e)
            Result.failure(Exception("서버에 연결할 수 없습니다."))
        } catch (e: Exception) {
            Log.e(TAG, "매장 목록 조회 실패", e)
            Result.failure(e)
        }
    }

    suspend fun getStoresForSubscription(
        dateStart: String,
        dateEnd: String,
        mon: Boolean,
        tue: Boolean,
        wed: Boolean,
        thu: Boolean,
        fri: Boolean,
        sat: Boolean,
        sun: Boolean
    ): Result<List<Store>> {
        return try {
            val response = storeApi.getStoresForSubscription(
                dateStart = dateStart,
                dateEnd = dateEnd,
                mon = mon,
                tue = tue,
                wed = wed,
                thu = thu,
                fri = fri,
                sat = sat,
                sun = sun
            )

            if (response.isSuccessful) {
                Result.success(response.body().orEmpty())
            } else {
                Result.failure(Exception("구독 매장 목록 조회 실패: code=${response.code()}"))
            }
        } catch (e: IOException) {
            Log.e(TAG, "구독 매장 목록 네트워크 실패", e)
            Result.failure(Exception("서버에 연결할 수 없습니다."))
        } catch (e: Exception) {
            Log.e(TAG, "구독 매장 목록 조회 실패", e)
            Result.failure(e)
        }
    }

    suspend fun getPickupPoints(storeId: Long, pickupDate: String? = null): Result<List<PickupPoint>> {
        return try {
            val response = storeApi.getPickupPoints(storeId, pickupDate)

            if (response.isSuccessful) {
                Result.success(response.body().orEmpty())
            } else if (response.code() == 204) {
                Result.success(emptyList())
            } else {
                Result.failure(Exception("픽업 포인트 조회 실패: code=${response.code()}"))
            }
        } catch (e: IOException) {
            Log.e(TAG, "픽업 포인트 네트워크 실패", e)
            Result.failure(Exception("서버에 연결할 수 없습니다."))
        } catch (e: Exception) {
            Log.e(TAG, "픽업 포인트 조회 실패", e)
            Result.failure(e)
        }
    }

    suspend fun getPickupPointsInBounds(
        south: Double,
        north: Double,
        west: Double,
        east: Double,
        pickupDate: String? = null
    ): Result<List<PickupPoint>> {
        return try {
            val response = storeApi.getPickupPointsInBounds(
                south = south,
                north = north,
                west = west,
                east = east,
                pickupDate = pickupDate
            )

            if (response.isSuccessful) {
                Result.success(response.body().orEmpty())
            } else if (response.code() == 204) {
                Result.success(emptyList())
            } else {
                Result.failure(Exception("픽업 포인트 조회 실패: code=${response.code()}"))
            }
        } catch (e: IOException) {
            Log.e(TAG, "픽업 포인트 네트워크 실패", e)
            Result.failure(Exception("서버에 연결할 수 없습니다."))
        } catch (e: Exception) {
            Log.e(TAG, "픽업 포인트 조회 실패", e)
            Result.failure(e)
        }
    }

    suspend fun getPickupPointCapacity(
        pickupPointId: Long,
        pickupDate: String
    ): Result<PickupPoint> {
        return try {
            val response = storeApi.getPickupPointCapacity(pickupPointId, pickupDate)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception("픽업 포인트 칸 정보 조회 실패: code=${response.code()}"))
            }
        } catch (e: IOException) {
            Log.e(TAG, "픽업 포인트 칸 정보 네트워크 실패", e)
            Result.failure(Exception("서버에 연결할 수 없습니다."))
        } catch (e: Exception) {
            Log.e(TAG, "픽업 포인트 칸 정보 조회 실패", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "StoreRepository"
    }
}
