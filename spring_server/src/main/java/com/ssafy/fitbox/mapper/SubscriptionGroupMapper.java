package com.ssafy.fitbox.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.fitbox.dto.SubscriptionGroup;
import com.ssafy.fitbox.dto.response.SubscriptionResponse;

@Mapper
public interface SubscriptionGroupMapper {

    int insert(SubscriptionGroup subscriptionGroup);

    SubscriptionGroup selectById(Long id);

    List<SubscriptionResponse> findSubscriptionsByUserId(
            @Param("userId") Integer userId
    );

    int cancelSubscription(
            @Param("subscriptionGroupId") Long subscriptionGroupId,
            @Param("userId") Integer userId
    );
}
