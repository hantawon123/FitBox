package com.ssafy.fitbox.mapper;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.fitbox.dto.PickupPoint;
import com.ssafy.fitbox.dto.Store;

@Mapper
public interface StoreMapper {

    ArrayList<Store> selectAll();

    Store selectById(Long id);

    int insert(Store storeDto);

    int update(Store storeDto);

    int delete(Long id);

    ArrayList<Store> findStoresByPickupDate(
            @Param("pickupDate") String pickupDate
    );
    ArrayList<Store> findStoresByPickupDates(
            @Param("pickupDates") List<String> pickupDates
    );

    ArrayList<PickupPoint> findPickupPointsByStoreId(
            @Param("storeId") Long storeId,
            @Param("pickupDate") String pickupDate
    );

    ArrayList<PickupPoint> findPickupPointsByBounds(
            @Param("south") Double south,
            @Param("north") Double north,
            @Param("west") Double west,
            @Param("east") Double east,
            @Param("pickupDate") String pickupDate
    );

    PickupPoint findPickupPointCapacity(
            @Param("pickupPointId") Long pickupPointId,
            @Param("pickupDate") String pickupDate
    );
}
