package com.ssafy.fitbox.repository

import android.util.Log
import com.ssafy.fitbox.network.RetrofitClient
import com.ssafy.fitbox.network.api.OrderApi
import com.ssafy.fitbox.network.request.DirectOrderRequest
import com.ssafy.fitbox.network.request.OrderCartRequest
import com.ssafy.fitbox.network.request.OrderStatusUpdateRequest
import com.ssafy.fitbox.network.request.LockerAssignmentRequest
import com.ssafy.fitbox.network.request.NfcPickupRequest
import com.ssafy.fitbox.network.request.SelectedDateSubscriptionOrderRequest
import com.ssafy.fitbox.network.request.SubscriptionCreateRequest
import com.ssafy.fitbox.network.request.SubscriptionOrderRequest
import com.ssafy.fitbox.network.response.OrderResponse
import com.ssafy.fitbox.network.response.SubscriptionOrderResponse
import com.ssafy.fitbox.network.response.SubscriptionResponse
import java.io.IOException

class OrderRepository(
    private val orderApi: OrderApi = RetrofitClient.orderApi
) {

    suspend fun orderDirect(
        request: DirectOrderRequest
    ): Result<OrderResponse> {
        return try {
            val response = orderApi.orderDirect(request)

            if (response.isSuccessful) {
                val body = response.body()

                if (body == null) {
                    Result.failure(Exception("주문 응답이 없습니다."))
                } else {
                    Result.success(body)
                }
            } else {
                Result.failure(Exception("바로 주문 실패: code=${response.code()}"))
            }
        } catch (e: IOException) {
            Log.e(TAG, "바로 주문 네트워크 실패", e)
            Result.failure(Exception("서버에 연결할 수 없습니다."))
        } catch (e: Exception) {
            Log.e(TAG, "바로 주문 실패", e)
            Result.failure(e)
        }
    }

    suspend fun orderFromCart(
        request: OrderCartRequest
    ): Result<List<OrderResponse>> {
        return try {
            val response = orderApi.orderFromCart(request)

            if (response.isSuccessful) {
                Result.success(response.body().orEmpty())
            } else {
                Result.failure(Exception("장바구니 주문 실패: code=${response.code()}"))
            }
        } catch (e: IOException) {
            Log.e(TAG, "장바구니 주문 네트워크 실패", e)
            Result.failure(Exception("서버에 연결할 수 없습니다."))
        } catch (e: Exception) {
            Log.e(TAG, "장바구니 주문 실패", e)
            Result.failure(e)
        }
    }

    suspend fun orderSubscription(
        request: SubscriptionOrderRequest
    ): Result<SubscriptionOrderResponse> {
        return try {
            val response = orderApi.orderSubscription(request)

            if (response.isSuccessful) {
                val body = response.body()

                if (body == null) {
                    Result.failure(Exception("구독 주문 응답이 없습니다."))
                } else {
                    Result.success(body)
                }
            } else {
                Result.failure(Exception("구독 주문 실패: code=${response.code()}"))
            }
        } catch (e: IOException) {
            Log.e(TAG, "구독 주문 네트워크 실패", e)
            Result.failure(Exception("서버에 연결할 수 없습니다."))
        } catch (e: Exception) {
            Log.e(TAG, "구독 주문 실패", e)
            Result.failure(e)
        }
    }

    suspend fun orderSelectedDateSubscription(
        request: SelectedDateSubscriptionOrderRequest
    ): Result<List<SubscriptionOrderResponse>> {
        return try {
            val response = orderApi.orderSelectedDateSubscription(request)

            if (response.isSuccessful) {
                Result.success(response.body().orEmpty())
            } else {
                Result.failure(Exception("선택 날짜 구독 주문 실패: code=${response.code()}"))
            }
        } catch (e: IOException) {
            Log.e(TAG, "선택 날짜 구독 주문 네트워크 실패", e)
            Result.failure(Exception("서버에 연결할 수 없습니다."))
        } catch (e: Exception) {
            Log.e(TAG, "선택 날짜 구독 주문 실패", e)
            Result.failure(e)
        }
    }

    suspend fun createMonthlySubscription(
        request: SubscriptionCreateRequest
    ): Result<SubscriptionResponse> {
        return try {
            val response = orderApi.createMonthlySubscription(request)

            if (response.isSuccessful) {
                val body = response.body()

                if (body == null) {
                    Result.failure(Exception("정기 구독 응답이 없습니다."))
                } else {
                    Result.success(body)
                }
            } else {
                val errorMessage = response.errorBody()?.string()
                Result.failure(
                    Exception(
                        errorMessage?.takeIf { it.isNotBlank() }
                            ?: "정기 구독 생성 실패: code=${response.code()}"
                    )
                )
            }
        } catch (e: IOException) {
            Log.e(TAG, "정기 구독 네트워크 실패", e)
            Result.failure(Exception("서버에 연결할 수 없습니다."))
        } catch (e: Exception) {
            Log.e(TAG, "정기 구독 생성 실패", e)
            Result.failure(e)
        }
    }

    suspend fun getUserSubscriptions(
        userId: Int
    ): Result<List<SubscriptionResponse>> {
        return try {
            val response = orderApi.getUserSubscriptions(userId)

            if (response.isSuccessful) {
                Result.success(response.body().orEmpty())
            } else if (response.code() == 204) {
                Result.success(emptyList())
            } else {
                Result.failure(Exception("구독 상품 조회 실패: code=${response.code()}"))
            }
        } catch (e: IOException) {
            Log.e(TAG, "구독 상품 조회 네트워크 실패", e)
            Result.failure(Exception("서버에 연결할 수 없습니다."))
        } catch (e: Exception) {
            Log.e(TAG, "구독 상품 조회 실패", e)
            Result.failure(e)
        }
    }

    suspend fun cancelSubscription(
        subscriptionGroupId: Long,
        userId: Int
    ): Result<Unit> {
        return try {
            val response = orderApi.cancelSubscription(subscriptionGroupId, userId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(
                    Exception(response.errorBody()?.string() ?: "구독 취소에 실패했습니다.")
                )
            }
        } catch (e: IOException) {
            Result.failure(Exception("서버에 연결할 수 없습니다."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserOrders(userId: Long): Result<List<OrderResponse>> {
        return try {
            val response = orderApi.getUserOrders(userId)

            if (response.isSuccessful) {
                Result.success(response.body().orEmpty())
            } else {
                Result.failure(Exception("주문 내역 조회 실패: code=${response.code()}"))
            }
        } catch (e: IOException) {
            Log.e(TAG, "주문 내역 네트워크 실패", e)
            Result.failure(Exception("서버에 연결할 수 없습니다."))
        } catch (e: Exception) {
            Log.e(TAG, "주문 내역 조회 실패", e)
            Result.failure(e)
        }
    }

    suspend fun getAdminOrders(): Result<List<OrderResponse>> {
        return try {
            val response = orderApi.getAdminOrders()
            if (response.isSuccessful) {
                Result.success(response.body().orEmpty())
            } else {
                Result.failure(Exception("관리자 주문 조회 실패: code=${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "관리자 주문 조회 실패", e)
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(
        orderId: Long,
        orderStatus: String
    ): Result<OrderResponse> {
        return try {
            val response = orderApi.updateOrderStatus(
                orderId = orderId,
                request = OrderStatusUpdateRequest(orderStatus)
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body == null) {
                    Result.failure(Exception("주문 상태 변경 응답이 없습니다."))
                } else {
                    Result.success(body)
                }
            } else {
                Result.failure(
                    Exception(response.errorBody()?.string() ?: "주문 상태 변경 실패: code=${response.code()}")
                )
            }
        } catch (e: IOException) {
            Log.e(TAG, "주문 상태 변경 네트워크 실패", e)
            Result.failure(Exception("서버에 연결할 수 없습니다."))
        } catch (e: Exception) {
            Log.e(TAG, "주문 상태 변경 실패", e)
            Result.failure(e)
        }
    }

    suspend fun assignLocker(orderId: Long, lockerNumber: String): Result<OrderResponse> {
        return runCatching {
            val response = orderApi.assignLocker(
                orderId,
                LockerAssignmentRequest(lockerNumber)
            )
            if (!response.isSuccessful) {
                error(response.errorBody()?.string() ?: "사물함 할당에 실패했습니다.")
            }
            response.body() ?: error("사물함 할당 응답이 없습니다.")
        }
    }

    suspend fun completeNfcPickup(
        orderId: Long,
        userId: Int,
        pickupPointName: String,
        lockerNumber: String
    ): Result<OrderResponse> {
        return runCatching {
            val response = orderApi.completeNfcPickup(
                orderId,
                NfcPickupRequest(userId, pickupPointName, lockerNumber)
            )
            if (!response.isSuccessful) {
                error(response.errorBody()?.string() ?: "주문정보와 일치하지 않습니다.")
            }
            response.body() ?: error("NFC 픽업 응답이 없습니다.")
        }
    }

    companion object {
        private const val TAG = "OrderRepository"
    }
}
