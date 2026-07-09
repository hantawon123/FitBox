package com.ssafy.fitbox.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ssafy.fitbox.dto.CartItem
import com.ssafy.fitbox.network.request.CartItemRequest
import com.ssafy.fitbox.repository.CartRepository
import com.ssafy.fitbox.util.SessionManager
import kotlinx.coroutines.launch

class CartViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val cartRepository = CartRepository()
    private val userSessionManager = SessionManager(application)

    private val _cartItems = MutableLiveData<List<CartItem>>(emptyList())
    val cartItems: LiveData<List<CartItem>> = _cartItems

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    fun loadCartItems() {
        val userId = getLoginUserId()
        if (userId == null) {
            _cartItems.value = emptyList()
            return
        }

        viewModelScope.launch {
            val result = cartRepository.getCartItems(userId)

            result.onSuccess { items ->
                _cartItems.value = items
            }.onFailure { exception ->
                _message.value = exception.message ?: "장바구니 조회에 실패했습니다"
            }
        }
    }

    fun addCartItem(request: CartItemRequest) {
        val userId = getLoginUserId()
        if (userId == null) {
            _message.value = "로그인이 필요합니다"
            return
        }

        viewModelScope.launch {
            val result = cartRepository.addCartItem(
                userId = userId,
                request = request
            )

            result.onSuccess { items ->
                _cartItems.value = items
                _message.value = "장바구니에 담겼습니다"
            }.onFailure { exception ->
                _message.value = exception.message ?: "장바구니 담기에 실패했습니다"
            }
        }
    }

    fun deleteCartItem(cartItem: CartItem) {
        viewModelScope.launch {
            val result = cartRepository.deleteCartItem(cartItem.id)

            result.onSuccess {
                loadCartItems()
            }.onFailure { exception ->
                _message.value = exception.message ?: "장바구니 삭제에 실패했습니다"
            }
        }
    }

    fun clearCart() {
        val userId = getLoginUserId()
        if (userId == null) {
            _cartItems.value = emptyList()
            return
        }

        viewModelScope.launch {
            val result = cartRepository.clearCart(userId)

            result.onSuccess {
                _cartItems.value = emptyList()
                _message.value = "장바구니를 비웠습니다"
            }.onFailure { exception ->
                _message.value = exception.message ?: "장바구니 비우기에 실패했습니다"
            }
        }
    }

    fun getTotalPrice(): Int {
        return _cartItems.value
            .orEmpty()
            .sumOf { cartItem ->
                cartItem.price * cartItem.quantity
            }
    }

    fun isEmpty(): Boolean {
        return _cartItems.value.orEmpty().isEmpty()
    }

    private fun getLoginUserId(): Int? {
        return userSessionManager.getUser()?.id
    }
}
