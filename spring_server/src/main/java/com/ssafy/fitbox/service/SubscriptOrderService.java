package com.ssafy.fitbox.service;

import java.util.ArrayList;
import java.util.List;

import com.ssafy.fitbox.dto.SubscriptOrder;
import com.ssafy.fitbox.dto.request.SelectedDateSubscriptOrderRequest;
import com.ssafy.fitbox.dto.request.SubscriptionCreateRequest;
import com.ssafy.fitbox.dto.request.SubscriptionMultiDateOrderRequest;
import com.ssafy.fitbox.dto.response.SubscriptionResponse;

public interface SubscriptOrderService {

    ArrayList<SubscriptOrder> selectAll();

    SubscriptOrder selectById(Long id);

    int insert(SubscriptOrder subscriptOrder);

    int update(SubscriptOrder subscriptOrder);

    int delete(Long id);

    List<SubscriptOrder> insertSelectedDateOrders(
            SelectedDateSubscriptOrderRequest request
    );

    List<SubscriptOrder> insertMultiDateOrders(
            SubscriptionMultiDateOrderRequest request
    );

    SubscriptionResponse createMonthlySubscription(
            SubscriptionCreateRequest request
    );

    List<SubscriptionResponse> getUserSubscriptions(
            Integer userId
    );

    void cancelSubscription(
            Long subscriptionGroupId,
            Integer userId
    );
}
