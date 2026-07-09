package com.ssafy.fitbox.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.fitbox.dto.PickupPoint
import com.ssafy.fitbox.dto.Store
import com.ssafy.fitbox.repository.StoreRepository
import kotlinx.coroutines.launch

class StoreViewModel(
    private val storeRepository: StoreRepository = StoreRepository()
) : ViewModel() {

    private val _stores = MutableLiveData<List<Store>>()
    val stores: LiveData<List<Store>> = _stores

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _pickupPoints = MutableLiveData<List<PickupPoint>>()
    val pickupPoints: LiveData<List<PickupPoint>> = _pickupPoints

    fun loadStores(pickupDate: String) {
        viewModelScope.launch {
            val result = storeRepository.getStores(pickupDate)

            result.onSuccess { storeList ->
                _stores.value = storeList
            }.onFailure { throwable ->
                _errorMessage.value = throwable.message ?: "매장 목록을 불러오지 못했습니다."
            }
        }
    }

    fun loadStoresForSubscription(
        dateStart: String,
        dateEnd: String,
        mon: Boolean,
        tue: Boolean,
        wed: Boolean,
        thu: Boolean,
        fri: Boolean,
        sat: Boolean,
        sun: Boolean
    ) {
        viewModelScope.launch {
            val result = storeRepository.getStoresForSubscription(
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

            result.onSuccess { storeList ->
                _stores.value = storeList
            }.onFailure { throwable ->
                _errorMessage.value = throwable.message ?: "구독 매장 목록을 불러오지 못했습니다."
            }
        }
    }

    fun loadPickupPoints(storeId: Long, pickupDate: String? = null) {
        viewModelScope.launch {
            val result = storeRepository.getPickupPoints(storeId, pickupDate)

            result.onSuccess { pickupPointList ->
                _pickupPoints.value = pickupPointList
            }.onFailure { throwable ->
                _errorMessage.value = throwable.message ?: "픽업 포인트를 불러오지 못했습니다."
            }
        }
    }

    fun loadPickupPointsInBounds(
        south: Double,
        north: Double,
        west: Double,
        east: Double,
        pickupDate: String? = null
    ) {
        viewModelScope.launch {
            val result = storeRepository.getPickupPointsInBounds(
                south = south,
                north = north,
                west = west,
                east = east,
                pickupDate = pickupDate
            )

            result.onSuccess { pickupPointList ->
                _pickupPoints.value = pickupPointList
            }.onFailure { throwable ->
                _errorMessage.value = throwable.message ?: "픽업 포인트를 불러오지 못했습니다."
            }
        }
    }
}
