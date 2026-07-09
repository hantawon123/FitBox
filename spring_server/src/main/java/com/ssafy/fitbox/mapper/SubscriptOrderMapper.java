package com.ssafy.fitbox.mapper;

import java.util.ArrayList;

import org.apache.ibatis.annotations.Mapper;

import com.ssafy.fitbox.dto.SubscriptOrder;

@Mapper
public interface SubscriptOrderMapper {

    ArrayList<SubscriptOrder> selectAll();

    SubscriptOrder selectById(Long id);

    int insert(SubscriptOrder subscriptOrder);

    int update(SubscriptOrder subscriptOrder);

    int delete(Long id);
}