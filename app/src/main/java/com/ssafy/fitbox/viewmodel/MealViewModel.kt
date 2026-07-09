package com.ssafy.fitbox.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.fitbox.dto.Product
import com.ssafy.fitbox.network.request.CustomMealCreateRequest
import com.ssafy.fitbox.network.response.CustomMealCreateResponse
import com.ssafy.fitbox.repository.MealRepository
import kotlinx.coroutines.launch

class MealViewModel(
    private val mealRepository: MealRepository = MealRepository()
) : ViewModel() {

    private val _customMealCreateResult =
        MutableLiveData<Result<CustomMealCreateResponse>>()

    val customMealCreateResult: LiveData<Result<CustomMealCreateResponse>> =
        _customMealCreateResult

    private val _productMeals = MutableLiveData<List<Product>>()
    val productMeals: LiveData<List<Product>> = _productMeals

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun loadProductMeals() {
        viewModelScope.launch {
            val result = mealRepository.getProductMealsCached()

            result.onSuccess { products ->
                _productMeals.value = products
            }.onFailure { throwable ->
                _errorMessage.value =
                    throwable.message ?: "완제품 식단을 불러오지 못했습니다."
            }
        }
    }

    fun createCustomMeal(request: CustomMealCreateRequest) {
        viewModelScope.launch {
            _customMealCreateResult.value =
                mealRepository.createCustomMeal(request)
        }
    }
}
