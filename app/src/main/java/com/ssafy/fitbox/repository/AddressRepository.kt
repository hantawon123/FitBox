package com.ssafy.fitbox.repository

import com.ssafy.fitbox.dto.Address
import com.ssafy.fitbox.network.RetrofitClient
import com.ssafy.fitbox.network.api.AddressApi
import com.ssafy.fitbox.util.AddressParts
import java.io.IOException

class AddressRepository(
    private val addressApi: AddressApi = RetrofitClient.addressApi
) {
    suspend fun getUserAddresses(userId: Int): Result<List<Address>> {
        return try {
            val response = addressApi.getUserAddresses(userId)
            if (response.isSuccessful) {
                Result.success(response.body().orEmpty())
            } else {
                Result.failure(Exception("배송지 조회 실패: code=${response.code()}"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("서버에 연결할 수 없습니다."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createAddress(userId: Int, address: String): Result<Address> {
        val parts = AddressParts.parse(address)
        return try {
            val response = addressApi.createAddress(
                Address(
                    userId = userId,
                    address = parts.fullAddress,
                    zoneCode = parts.zoneCode,
                    roadAddress = parts.roadAddress,
                    detailAddress = parts.detailAddress
                )
            )
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("저장된 배송지 응답이 없습니다."))
            } else {
                Result.failure(
                    Exception(response.errorBody()?.string() ?: "배송지 등록 실패: code=${response.code()}")
                )
            }
        } catch (e: IOException) {
            Result.failure(Exception("서버에 연결할 수 없습니다."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAddress(id: Int): Result<Unit> {
        return try {
            val response = addressApi.deleteAddress(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("배송지 삭제 실패: code=${response.code()}"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("서버에 연결할 수 없습니다."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
