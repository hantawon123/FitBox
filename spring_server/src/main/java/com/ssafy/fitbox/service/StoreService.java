package com.ssafy.fitbox.service;

import java.util.*;
import com.ssafy.fitbox.dto.PickupPoint;
import com.ssafy.fitbox.dto.Store;

public interface StoreService {

    ArrayList<Store> selectAll();

    Store selectById(Long id);

    int insert(Store store);

    int update(Store store);

    int delete(Long id);

    ArrayList<Store> findStoresByPickupDate(String pickupDate);

    ArrayList<Store> findStoresBySubscriptionCondition(
            String dateStart,
            String dateEnd,
            Boolean mon,
            Boolean tue,
            Boolean wed,
            Boolean thu,
            Boolean fri,
            Boolean sat,
            Boolean sun
    );

    ArrayList<PickupPoint> findPickupPointsByStoreId(Long storeId, String pickupDate);

    ArrayList<PickupPoint> findPickupPointsByBounds(
            Double south,
            Double north,
            Double west,
            Double east,
            String pickupDate
    );

    PickupPoint findPickupPointCapacity(Long pickupPointId, String pickupDate);
   
}
