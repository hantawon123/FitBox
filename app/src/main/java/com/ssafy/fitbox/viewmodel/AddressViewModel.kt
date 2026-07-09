package com.ssafy.fitbox.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.fitbox.dto.Address
import com.ssafy.fitbox.repository.AddressRepository
import kotlinx.coroutines.launch

class AddressViewModel : ViewModel() {
    private val repository = AddressRepository()

    private val _addresses = MutableLiveData<List<Address>>(emptyList())
    val addresses: LiveData<List<Address>> = _addresses

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    fun loadAddresses(userId: Int) {
        viewModelScope.launch {
            repository.getUserAddresses(userId)
                .onSuccess { _addresses.value = it }
                .onFailure { _message.value = it.message ?: "배송지를 불러오지 못했습니다." }
        }
    }

    fun addAddress(userId: Int, address: String) {
        viewModelScope.launch {
            repository.createAddress(userId, address)
                .onSuccess {
                    _message.value = "배송지가 추가되었습니다."
                    loadAddresses(userId)
                }
                .onFailure { _message.value = it.message ?: "배송지를 추가하지 못했습니다." }
        }
    }

    fun deleteAddress(userId: Int, id: Int) {
        viewModelScope.launch {
            repository.deleteAddress(id)
                .onSuccess {
                    _message.value = "배송지가 삭제되었습니다."
                    loadAddresses(userId)
                }
                .onFailure { _message.value = it.message ?: "배송지를 삭제하지 못했습니다." }
        }
    }
}
