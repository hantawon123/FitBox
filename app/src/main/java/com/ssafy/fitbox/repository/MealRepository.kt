package com.ssafy.fitbox.repository

import android.util.Log
import com.ssafy.fitbox.dto.Product
import com.ssafy.fitbox.network.RetrofitClient
import com.ssafy.fitbox.network.api.MealApi
import com.ssafy.fitbox.network.request.CustomMealCreateRequest
import com.ssafy.fitbox.network.response.CustomMealCreateResponse
import java.io.IOException
import com.ssafy.fitbox.util.MealLocalCache
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MealRepository(
    private val mealApi: MealApi = RetrofitClient.mealApi
) {
    suspend fun getProductMealsCached(): Result<List<Product>> {
        MealLocalCache.getProducts(KEY_PRODUCTS)?.let { return Result.success(it) }
        return cacheMutex.withLock {
            MealLocalCache.getProducts(KEY_PRODUCTS)?.let { return@withLock Result.success(it) }
            getProductMeals().onSuccess { MealLocalCache.putProducts(KEY_PRODUCTS, it) }
        }
    }

    suspend fun getPopularMealsCached(): Result<List<Product>> {
        MealLocalCache.getProducts(KEY_POPULAR)?.let { return Result.success(it) }
        return cacheMutex.withLock {
            MealLocalCache.getProducts(KEY_POPULAR)?.let { return@withLock Result.success(it) }
            getPopularMeals().onSuccess { MealLocalCache.putProducts(KEY_POPULAR, it) }
        }
    }

    suspend fun getMonthlyPopularMealsCached(): Result<List<Product>> {
        MealLocalCache.getProducts(KEY_MONTHLY_POPULAR)?.let { return Result.success(it) }
        return cacheMutex.withLock {
            MealLocalCache.getProducts(KEY_MONTHLY_POPULAR)?.let {
                return@withLock Result.success(it)
            }
            getMonthlyPopularMeals().onSuccess {
                MealLocalCache.putProducts(KEY_MONTHLY_POPULAR, it)
            }
        }
    }

    suspend fun getPopularMealsBySamePurposeCached(userId: Int): Result<List<Product>> {
        val key = "${KEY_PURPOSE}_$userId"
        MealLocalCache.getProducts(key)?.let { return Result.success(it) }
        return cacheMutex.withLock {
            MealLocalCache.getProducts(key)?.let { return@withLock Result.success(it) }
            getPopularMealsBySamePurpose(userId).onSuccess {
                MealLocalCache.putProducts(key, it)
            }
        }
    }

    suspend fun getTodayRecommendedProductCached(): Result<Product> {
        MealLocalCache.getProduct(KEY_RECOMMEND)?.let { return Result.success(it) }
        return cacheMutex.withLock {
            MealLocalCache.getProduct(KEY_RECOMMEND)?.let { return@withLock Result.success(it) }
            getTodayRecommendedProduct().onSuccess {
                MealLocalCache.putProduct(KEY_RECOMMEND, it)
            }
        }
    }

    suspend fun warmHomeCache(userId: Int?) {
        getProductMealsCached()
        if (userId == null) {
            getPopularMealsCached()
        } else {
            getPopularMealsBySamePurposeCached(userId)
        }
        getMonthlyPopularMealsCached()
        getTodayRecommendedProductCached()
    }

    suspend fun getProductMeals(): Result<List<Product>> {
        return try {
            val response = mealApi.getProductMeals()

            if (response.isSuccessful) {
                val products = response.body()
                    .orEmpty()
                    .map { it.toProduct() }

                Result.success(products)
            } else {
                Result.failure(Exception("완제품 식단 조회 실패: code=${response.code()}"))
            }
        } catch (e: IOException) {
            Log.e(TAG, "완제품 식단 네트워크 실패", e)
            Result.failure(Exception("서버에 연결할 수 없습니다."))
        } catch (e: Exception) {
            Log.e(TAG, "완제품 식단 조회 실패", e)
            Result.failure(e)
        }
    }

    suspend fun getMealById(mealId: Long): Result<Product> {
        return try {
            val response = mealApi.getMealById(mealId)

            if (response.isSuccessful) {
                val product = response.body()?.toProduct()
                if (product == null) {
                    Result.failure(Exception("식단 정보를 찾을 수 없습니다."))
                } else {
                    Result.success(product)
                }
            } else {
                Result.failure(Exception("식단 상세 조회 실패: code=${response.code()}"))
            }
        } catch (e: IOException) {
            Log.e(TAG, "식단 상세 네트워크 실패", e)
            Result.failure(Exception("서버에 연결할 수 없습니다."))
        } catch (e: Exception) {
            Log.e(TAG, "식단 상세 조회 실패", e)
            Result.failure(e)
        }
    }

    suspend fun getTodayRecommendedProduct(): Result<Product> {
        return try {
            val response = mealApi.getTodayRecommendedProduct()

            if (response.isSuccessful) {
                val product = response.body()?.toProduct()

                if (product == null) {
                    Result.failure(Exception("추천 식단 정보가 없습니다."))
                } else {
                    Result.success(product)
                }
            } else {
                Result.failure(Exception("추천 식단 조회 실패: code=${response.code()}"))
            }
        } catch (e: IOException) {
            Log.e(TAG, "추천 식단 네트워크 실패", e)
            Result.failure(Exception("서버에 연결할 수 없습니다."))
        } catch (e: Exception) {
            Log.e(TAG, "추천 식단 조회 실패", e)
            Result.failure(e)
        }
    }

    suspend fun getPopularMeals(): Result<List<Product>> {
        return try {
            val response = mealApi.getPopularMeals()

            if (response.isSuccessful) {
                val products = response.body()
                    .orEmpty()
                    .map { it.toProduct() }

                Result.success(products)
            } else {
                Result.failure(Exception("인기 식단 조회 실패: code=${response.code()}"))
            }
        } catch (e: IOException) {
            Log.e(TAG, "인기 식단 네트워크 실패", e)
            Result.failure(Exception("서버에 연결할 수 없습니다."))
        } catch (e: Exception) {
            Log.e(TAG, "인기 식단 조회 실패", e)
            Result.failure(e)
        }
    }

    suspend fun getMonthlyPopularMeals(): Result<List<Product>> {
        return try {
            val response = mealApi.getMonthlyPopularMeals()

            if (response.isSuccessful) {
                Result.success(
                    response.body()
                        .orEmpty()
                        .map { it.toProduct() }
                )
            } else {
                Result.failure(Exception("이번 달 인기 식단 조회 실패: code=${response.code()}"))
            }
        } catch (e: IOException) {
            Log.e(TAG, "이번 달 인기 식단 네트워크 실패", e)
            Result.failure(Exception("서버에 연결할 수 없습니다."))
        } catch (e: Exception) {
            Log.e(TAG, "이번 달 인기 식단 조회 실패", e)
            Result.failure(e)
        }
    }

    suspend fun getPopularMealsBySamePurpose(userId: Int): Result<List<Product>> {
        return try {
            val response = mealApi.getPopularMealsBySamePurpose(userId)

            if (response.isSuccessful) {
                val products = response.body()
                    .orEmpty()
                    .map { it.toProduct() }

                Result.success(products)
            } else {
                Result.failure(Exception("같은 목적 인기 식단 조회 실패: code=${response.code()}"))
            }
        } catch (e: IOException) {
            Log.e(TAG, "같은 목적 인기 식단 네트워크 실패", e)
            Result.failure(Exception("서버에 연결할 수 없습니다."))
        } catch (e: Exception) {
            Log.e(TAG, "같은 목적 인기 식단 조회 실패", e)
            Result.failure(e)
        }
    }

    suspend fun createCustomMeal(
        request: CustomMealCreateRequest
    ): Result<CustomMealCreateResponse> {
        return try {
            val response = mealApi.createCustomMeal(request)

            if (response.isSuccessful) {
                val body = response.body()

                if (body == null) {
                    Result.failure(Exception("커스텀 식단 생성 응답이 없습니다."))
                } else {
                    Result.success(body)
                }
            } else {
                Result.failure(Exception("커스텀 식단 생성 실패: code=${response.code()}"))
            }
        } catch (e: IOException) {
            Log.e(TAG, "커스텀 식단 생성 네트워크 실패", e)
            Result.failure(Exception("서버에 연결할 수 없습니다."))
        } catch (e: Exception) {
            Log.e(TAG, "커스텀 식단 생성 실패", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "MealRepository"
        private const val KEY_PRODUCTS = "products"
        private const val KEY_POPULAR = "popular"
        private const val KEY_MONTHLY_POPULAR = "monthly_popular"
        private const val KEY_PURPOSE = "purpose"
        private const val KEY_RECOMMEND = "recommend"
        private val cacheMutex = Mutex()
    }
}
