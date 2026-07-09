package com.ssafy.fitbox.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface StoreDailyCapacityMapper {

    int decreaseRemainCount(
            @Param("pickupPointId") Long pickupPointId,
            @Param("pickupDate") LocalDate pickupDate,
            @Param("count") int count
    );

    int increaseRemainCount(
            @Param("pickupPointId") Long pickupPointId,
            @Param("pickupDate") LocalDate pickupDate,
            @Param("count") int count
    );

    Integer findTotalCount(
            @Param("pickupPointId") Long pickupPointId,
            @Param("pickupDate") LocalDate pickupDate
    );
}
