package com.ssafy.fitbox.service;

import com.ssafy.fitbox.dto.Order;
import com.ssafy.fitbox.dto.request.DirectOrderRequest;
import com.ssafy.fitbox.dto.request.OrderCartRequest;
import com.ssafy.fitbox.dto.response.OrderResponse;

import java.util.ArrayList;
import java.util.List;

public interface OrderService {

    ArrayList<Order> selectAll();

    Order selectById(Long id);

    int insert(Order order);

    int update(Order order);

    int delete(Long id);

    List<OrderResponse> orderFromCart(OrderCartRequest request);

    OrderResponse orderDirect(DirectOrderRequest request);

    OrderResponse getOrder(Long orderId);

    List<OrderResponse> getUserOrders(Integer userId);

    List<OrderResponse> getAdminOrders();

    OrderResponse updateOrderStatus(Long orderId, String orderStatus);

    OrderResponse assignLocker(Long orderId, String lockerNumber);

    OrderResponse completeNfcPickup(
            Long orderId,
            Integer userId,
            String pickupPointName,
            String lockerNumber
    );
}
