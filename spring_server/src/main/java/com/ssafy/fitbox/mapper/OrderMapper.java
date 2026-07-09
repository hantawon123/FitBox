package com.ssafy.fitbox.mapper;

import com.ssafy.fitbox.dto.Order;
import com.ssafy.fitbox.dto.response.OrderResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

@Mapper
public interface OrderMapper {

    ArrayList<Order> selectAll();

    Order selectById(Long id);

    int insert(Order order);

    int update(Order order);

    int delete(Long id);

    int insertOrder(
            @Param("userId") Integer userId,
            @Param("mealId") Long mealId,
            @Param("quantity") Integer quantity,
            @Param("receiveType") String receiveType,
            @Param("receiveDate") String receiveDate,
            @Param("storeId") Long storeId,
            @Param("pickupPointId") Long pickupPointId,
            @Param("address") String address,
            @Param("orderTime") LocalDateTime orderTime
    );

    Long findLastInsertId();

    int insertPayment(
            @Param("orderId") Long orderId,
            @Param("paymentMethod") String paymentMethod,
            @Param("paymentStatus") String paymentStatus,
            @Param("amount") Integer amount
    );

    OrderResponse findOrderById(
            @Param("orderId") Long orderId
    );

    List<OrderResponse> findOrdersByUserId(
            @Param("userId") Integer userId
    );

    List<OrderResponse> findAllSingleOrders();

    int updateOrderStatus(
            @Param("orderId") Long orderId,
            @Param("orderStatus") String orderStatus
    );

    int updateMatchingOrderStatus(
            @Param("order") OrderResponse order,
            @Param("orderStatus") String orderStatus
    );

    int assignLocker(
            @Param("orderId") Long orderId,
            @Param("lockerNumber") String lockerNumber
    );

    int assignLockerToMatchingOrders(
            @Param("order") OrderResponse order,
            @Param("lockerNumber") String lockerNumber
    );

    int countActiveLocker(
            @Param("pickupPointId") Long pickupPointId,
            @Param("receiveDate") String receiveDate,
            @Param("lockerNumber") String lockerNumber,
            @Param("excludeOrderId") Long excludeOrderId
    );

    int countActiveLockerExcludingOrderGroup(
            @Param("order") OrderResponse order,
            @Param("lockerNumber") String lockerNumber
    );

    int completePickup(
            @Param("orderId") Long orderId
    );

    int completeMatchingPickup(
            @Param("order") OrderResponse order
    );
}
