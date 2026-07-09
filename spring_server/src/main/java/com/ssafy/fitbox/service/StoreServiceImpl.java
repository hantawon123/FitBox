package com.ssafy.fitbox.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ssafy.fitbox.dto.PickupPoint;
import com.ssafy.fitbox.dto.Store;
import com.ssafy.fitbox.mapper.StoreMapper;

@Service
public class StoreServiceImpl implements StoreService {

    private final StoreMapper storeMapper;

    public StoreServiceImpl(StoreMapper storeMapper) {
        this.storeMapper = storeMapper;
    }

    @Override
    public ArrayList<Store> selectAll() {
        return storeMapper.selectAll();
    }

    @Override
    public Store selectById(Long id) {
        return storeMapper.selectById(id);
    }

    @Override
    public int insert(Store store) {
        return storeMapper.insert(store);
    }

    @Override
    public int update(Store store) {
        return storeMapper.update(store);
    }

    @Override
    public int delete(Long id) {
        return storeMapper.delete(id);
    }

    @Override
    public ArrayList<Store> findStoresByPickupDate(String pickupDate) {
        if (pickupDate == null || pickupDate.isBlank()) {
            throw new IllegalArgumentException("pickupDate가 필요합니다.");
        }

        return storeMapper.findStoresByPickupDate(pickupDate);
    }

    @Override
    public ArrayList<Store> findStoresBySubscriptionCondition(
            String dateStart,
            String dateEnd,
            Boolean mon,
            Boolean tue,
            Boolean wed,
            Boolean thu,
            Boolean fri,
            Boolean sat,
            Boolean sun
    ) {
        if (dateStart == null || dateStart.isBlank()) {
            throw new IllegalArgumentException("구독 시작일이 필요합니다.");
        }

        if (dateEnd == null || dateEnd.isBlank()) {
            throw new IllegalArgumentException("구독 종료일이 필요합니다.");
        }

        List<String> pickupDates = calculateSubscriptionPickupDates(
                dateStart,
                dateEnd,
                mon,
                tue,
                wed,
                thu,
                fri,
                sat,
                sun
        );

        if (pickupDates.isEmpty()) {
            throw new IllegalArgumentException("선택된 구독 요일이 없습니다.");
        }

        return storeMapper.findStoresByPickupDates(pickupDates);
    }

    @Override
    public ArrayList<PickupPoint> findPickupPointsByStoreId(Long storeId, String pickupDate) {
        if (storeId == null) {
            throw new IllegalArgumentException("storeId가 필요합니다.");
        }

        return storeMapper.findPickupPointsByStoreId(storeId, pickupDate);
    }

    @Override
    public ArrayList<PickupPoint> findPickupPointsByBounds(
            Double south,
            Double north,
            Double west,
            Double east,
            String pickupDate
    ) {
        if (south == null || north == null || west == null || east == null) {
            throw new IllegalArgumentException("지도 경계 좌표가 필요합니다.");
        }

        double normalizedSouth = Math.min(south, north);
        double normalizedNorth = Math.max(south, north);
        double normalizedWest = Math.min(west, east);
        double normalizedEast = Math.max(west, east);

        return storeMapper.findPickupPointsByBounds(
                normalizedSouth,
                normalizedNorth,
                normalizedWest,
                normalizedEast,
                pickupDate
        );
    }

    @Override
    public PickupPoint findPickupPointCapacity(Long pickupPointId, String pickupDate) {
        if (pickupPointId == null) {
            throw new IllegalArgumentException("pickupPointId가 필요합니다.");
        }
        if (pickupDate == null || pickupDate.isBlank()) {
            throw new IllegalArgumentException("pickupDate가 필요합니다.");
        }
        return storeMapper.findPickupPointCapacity(pickupPointId, pickupDate);
    }

    private List<String> calculateSubscriptionPickupDates(
            String dateStart,
            String dateEnd,
            Boolean mon,
            Boolean tue,
            Boolean wed,
            Boolean thu,
            Boolean fri,
            Boolean sat,
            Boolean sun
    ) {
        LocalDate startDate = LocalDate.parse(dateStart);
        LocalDate endDate = LocalDate.parse(dateEnd);

        List<String> pickupDates = new ArrayList<>();

        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();

            if (isSelectedDay(dayOfWeek, mon, tue, wed, thu, fri, sat, sun)) {
                pickupDates.add(currentDate.toString());
            }

            currentDate = currentDate.plusDays(1);
        }

        return pickupDates;
    }

    private boolean isSelectedDay(
            DayOfWeek dayOfWeek,
            Boolean mon,
            Boolean tue,
            Boolean wed,
            Boolean thu,
            Boolean fri,
            Boolean sat,
            Boolean sun
    ) {
        return switch (dayOfWeek) {
            case MONDAY -> Boolean.TRUE.equals(mon);
            case TUESDAY -> Boolean.TRUE.equals(tue);
            case WEDNESDAY -> Boolean.TRUE.equals(wed);
            case THURSDAY -> Boolean.TRUE.equals(thu);
            case FRIDAY -> Boolean.TRUE.equals(fri);
            case SATURDAY -> Boolean.TRUE.equals(sat);
            case SUNDAY -> Boolean.TRUE.equals(sun);
        };
    }
}
