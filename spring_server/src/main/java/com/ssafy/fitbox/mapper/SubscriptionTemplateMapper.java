package com.ssafy.fitbox.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.fitbox.dto.SubscriptionTemplate;
import com.ssafy.fitbox.dto.response.SubscriptionTemplateResponse;

@Mapper
public interface SubscriptionTemplateMapper {

    int insert(SubscriptionTemplate subscriptionTemplate);

    List<SubscriptionTemplate> selectBySubscriptionGroupId(
            @Param("subscriptionGroupId") Long subscriptionGroupId
    );

    List<SubscriptionTemplateResponse> findTemplateResponsesBySubscriptionGroupId(
            @Param("subscriptionGroupId") Long subscriptionGroupId
    );
}