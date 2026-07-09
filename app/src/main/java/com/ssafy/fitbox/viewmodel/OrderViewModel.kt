package com.ssafy.fitbox.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.fitbox.network.request.DirectOrderRequest
import com.ssafy.fitbox.network.request.OrderCartRequest
import com.ssafy.fitbox.network.request.SelectedDateSubscriptionOrderRequest
import com.ssafy.fitbox.network.request.SubscriptionCreateRequest
import com.ssafy.fitbox.network.request.SubscriptionOrderRequest
import com.ssafy.fitbox.network.response.OrderResponse
import com.ssafy.fitbox.network.response.SubscriptionOrderResponse
import com.ssafy.fitbox.network.response.SubscriptionResponse
import com.ssafy.fitbox.repository.OrderRepository
import kotlinx.coroutines.launch

class OrderViewModel(
    private val orderRepository: OrderRepository = OrderRepository()
) : ViewModel() {

    private val _selectedDateSubscriptionOrderResult =
        MutableLiveData<Result<List<SubscriptionOrderResponse>>>()

    val selectedDateSubscriptionOrderResult:
            LiveData<Result<List<SubscriptionOrderResponse>>> =
        _selectedDateSubscriptionOrderResult

    private val _directOrderResult = MutableLiveData<Result<OrderResponse>>()
    val directOrderResult: LiveData<Result<OrderResponse>> = _directOrderResult

    private val _cartOrderResult = MutableLiveData<Result<List<OrderResponse>>>()
    val cartOrderResult: LiveData<Result<List<OrderResponse>>> = _cartOrderResult

    private val _subscriptionOrderResult = MutableLiveData<Result<SubscriptionOrderResponse>>()
    val subscriptionOrderResult: LiveData<Result<SubscriptionOrderResponse>> =
        _subscriptionOrderResult

    private val _monthlySubscriptionResult = MutableLiveData<Result<SubscriptionResponse>>()
    val monthlySubscriptionResult: LiveData<Result<SubscriptionResponse>> =
        _monthlySubscriptionResult

    private val _userSubscriptions = MutableLiveData<List<SubscriptionResponse>>()
    val userSubscriptions: LiveData<List<SubscriptionResponse>> = _userSubscriptions

    private val _cancelSubscriptionResult = MutableLiveData<Result<Unit>>()
    val cancelSubscriptionResult: LiveData<Result<Unit>> = _cancelSubscriptionResult

    private val _userOrders = MutableLiveData<List<OrderResponse>>()
    val userOrders: LiveData<List<OrderResponse>> = _userOrders

    private val _orderListErrorMessage = MutableLiveData<String>()
    val orderListErrorMessage: LiveData<String> = _orderListErrorMessage

    fun orderDirect(request: DirectOrderRequest) {
        viewModelScope.launch {
            _directOrderResult.value = orderRepository.orderDirect(request)
        }
    }

    fun orderFromCart(request: OrderCartRequest) {
        viewModelScope.launch {
            _cartOrderResult.value = orderRepository.orderFromCart(request)
        }
    }

    fun orderSubscription(request: SubscriptionOrderRequest) {
        viewModelScope.launch {
            _subscriptionOrderResult.value = orderRepository.orderSubscription(request)
        }
    }

    fun orderSelectedDateSubscription(
        request: SelectedDateSubscriptionOrderRequest
    ) {
        viewModelScope.launch {
            _selectedDateSubscriptionOrderResult.value =
                orderRepository.orderSelectedDateSubscription(request)
        }
    }

    fun createMonthlySubscription(
        request: SubscriptionCreateRequest
    ) {
        viewModelScope.launch {
            _monthlySubscriptionResult.value =
                orderRepository.createMonthlySubscription(request)
        }
    }

    fun getUserSubscriptions(userId: Int) {
        viewModelScope.launch {
            val result = orderRepository.getUserSubscriptions(userId)

            result.onSuccess { subscriptions ->
                _userSubscriptions.value = subscriptions
            }.onFailure { throwable ->
                _orderListErrorMessage.value =
                    throwable.message ?: "구독 상품을 불러오지 못했습니다."
            }
        }
    }

    fun cancelSubscription(subscriptionGroupId: Long, userId: Int) {
        viewModelScope.launch {
            _cancelSubscriptionResult.value =
                orderRepository.cancelSubscription(subscriptionGroupId, userId)
        }
    }

    fun getUserOrders(userId: Long) {
        viewModelScope.launch {
            val result = orderRepository.getUserOrders(userId)

            result.onSuccess { orders ->
                _userOrders.value = orders
            }.onFailure { throwable ->
                _orderListErrorMessage.value =
                    throwable.message ?: "주문 내역을 불러오지 못했습니다."
            }
        }
    }
}
