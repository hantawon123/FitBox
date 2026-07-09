package com.ssafy.fitbox.service;

import com.ssafy.fitbox.dto.Order;
import com.ssafy.fitbox.dto.request.DirectOrderRequest;
import com.ssafy.fitbox.dto.request.OrderCartRequest;
import com.ssafy.fitbox.dto.response.CartItemResponseDto;
import com.ssafy.fitbox.dto.response.CartResponseDto;
import com.ssafy.fitbox.dto.response.OrderResponse;
import com.ssafy.fitbox.mapper.OrderMapper;
import com.ssafy.fitbox.mapper.StoreDailyCapacityMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private static final String RECEIVE_TYPE_PICKUP = "PICKUP";
    private static final String RECEIVE_TYPE_PICKUP_POINT = "PICKUP_POINT";
    private static final String RECEIVE_TYPE_DELIVERY = "DELIVERY";
    private static final int DELIVERY_FEE = 3000;
    private static final int FREE_DELIVERY_THRESHOLD = 30000;
    private static final List<String> ORDER_STATUS_CODES = List.of(
            "ORDER_REVIEW",
            "PREPARING",
            "READY",
            "PICKED_UP"
    );

    private final OrderMapper orderMapper;
    private final CartService cartService;
    private final StoreDailyCapacityMapper storeDailyCapacityMapper;
    private final NotificationService notificationService;

    public OrderServiceImpl(
            OrderMapper orderMapper,
            CartService cartService,
            StoreDailyCapacityMapper storeDailyCapacityMapper,
            NotificationService notificationService
    ) {
        this.orderMapper = orderMapper;
        this.cartService = cartService;
        this.storeDailyCapacityMapper = storeDailyCapacityMapper;
        this.notificationService = notificationService;
    }

    @Override
    public ArrayList<Order> selectAll() {
        return orderMapper.selectAll();
    }

    @Override
    public Order selectById(Long id) {
        return orderMapper.selectById(id);
    }

    @Override
    public int insert(Order order) {
        return orderMapper.insert(order);
    }

    @Override
    public int update(Order order) {
        return orderMapper.update(order);
    }

    @Override
    public int delete(Long id) {
        return orderMapper.delete(id);
    }

    @Override
    @Transactional
    public List<OrderResponse> orderFromCart(OrderCartRequest request) {
        validateOrderRequest(
                request.getUserId(),
                request.getReceiveType(),
                request.getStoreId(),
                request.getPickupPointId(),
                request.getAddress(),
                request.getReceiveDate()
        );

        CartResponseDto cart = cartService.getCart(request.getUserId());

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("장바구니가 비어 있습니다.");
        }

        int totalQuantity = calculateCartTotalQuantity(cart.getItems());
        int totalProductAmount = calculateCartProductAmount(cart.getItems());
        int deliveryFee = calculateDeliveryFee(request.getReceiveType(), totalProductAmount);
        boolean deliveryFeeApplied = false;
        LocalDateTime cartOrderTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        decreasePickupCapacity(
                request.getReceiveType(),
                request.getPickupPointId(),
                request.getReceiveDate(),
                totalQuantity
        );

        List<OrderResponse> result = new ArrayList<>();

        for (CartItemResponseDto item : cart.getItems()) {
            int amount = item.getPrice() * item.getQuantity();
            int paymentAmount = amount;
            if (!deliveryFeeApplied) {
                paymentAmount += deliveryFee;
                deliveryFeeApplied = true;
            }

            orderMapper.insertOrder(
                    request.getUserId(),
                    item.getMealId(),
                    item.getQuantity(),
                    request.getReceiveType(),
                    request.getReceiveDate(),
                    request.getStoreId(),
                    request.getPickupPointId(),
                    getAddressIfDelivery(request.getReceiveType(), request.getAddress()),
                    cartOrderTime
            );

            Long orderId = orderMapper.findLastInsertId();

            orderMapper.insertPayment(
                    orderId,
                    getPaymentMethod(request.getPaymentMethod()),
                    "SUCCESS",
                    paymentAmount
            );

            OrderResponse orderResponse = orderMapper.findOrderById(orderId);
            result.add(orderResponse);
        }

        cartService.clearCart(request.getUserId());
        notifyNewStoreOrder(
                request.getStoreId(),
                newOrderMessage(
                        result.size() + "개 메뉴의 신규 주문이 들어왔습니다.",
                        request.getReceiveDate()
                )
        );

        return result;
    }

    @Override
    @Transactional
    public OrderResponse orderDirect(DirectOrderRequest request) {
        validateOrderRequest(
                request.getUserId(),
                request.getReceiveType(),
                request.getStoreId(),
                request.getPickupPointId(),
                request.getAddress(),
                request.getReceiveDate()
        );

        if (request.getMealId() == null) {
            throw new IllegalArgumentException("mealId가 필요합니다.");
        }

        int quantity = request.getQuantity() == null || request.getQuantity() <= 0
                ? 1
                : request.getQuantity();

        decreasePickupCapacity(
                request.getReceiveType(),
                request.getPickupPointId(),
                request.getReceiveDate(),
                quantity
        );

        orderMapper.insertOrder(
                request.getUserId(),
                request.getMealId(),
                quantity,
                request.getReceiveType(),
                    request.getReceiveDate(),
                    request.getStoreId(),
                    request.getPickupPointId(),
                    getAddressIfDelivery(request.getReceiveType(), request.getAddress()),
                    null
            );

        Long orderId = orderMapper.findLastInsertId();

        OrderResponse order = orderMapper.findOrderById(orderId);

        orderMapper.insertPayment(
                orderId,
                getPaymentMethod(request.getPaymentMethod()),
                "SUCCESS",
                order.getTotalPrice() + calculateDeliveryFee(
                        request.getReceiveType(),
                        order.getTotalPrice()
                )
        );

        OrderResponse completedOrder = orderMapper.findOrderById(orderId);
        notifyNewStoreOrder(
                request.getStoreId(),
                newOrderMessage(
                        completedOrder.getMealName() + " 신규 주문이 들어왔습니다.",
                        completedOrder.getReceiveDate()
                )
        );
        return completedOrder;
    }

    @Override
    public OrderResponse getOrder(Long orderId) {
        return orderMapper.findOrderById(orderId);
    }

    @Override
    public List<OrderResponse> getUserOrders(Integer userId) {
        return orderMapper.findOrdersByUserId(userId);
    }

    @Override
    public List<OrderResponse> getAdminOrders() {
        return orderMapper.findAllSingleOrders();
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, String orderStatus) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId가 필요합니다.");
        }

        if (orderStatus == null || !ORDER_STATUS_CODES.contains(orderStatus)) {
            throw new IllegalArgumentException("유효하지 않은 주문 상태입니다.");
        }

        OrderResponse order = requireOrder(orderId);
        int updated = orderMapper.updateMatchingOrderStatus(order, orderStatus);

        if (updated == 0) {
            throw new IllegalArgumentException("주문을 찾을 수 없습니다.");
        }

        return orderMapper.findOrderById(orderId);
    }

    @Override
    @Transactional
    public OrderResponse assignLocker(Long orderId, String lockerNumber) {
        OrderResponse order = requireOrder(orderId);
        if (!RECEIVE_TYPE_PICKUP_POINT.equals(order.getReceiveType())) {
            throw new IllegalArgumentException("픽업 포인트 주문만 사물함을 할당할 수 있습니다.");
        }
        String normalizedLockerNumber = normalizeRequired(lockerNumber, "사물함 번호");
        validateLockerNumberInCapacity(
                order.getPickupPointId(),
                order.getReceiveDate(),
                normalizedLockerNumber
        );
        if (orderMapper.countActiveLockerExcludingOrderGroup(
                order,
                normalizedLockerNumber
        ) > 0) {
            throw new IllegalArgumentException("이미 사용 중인 사물함입니다.");
        }
        int updated = orderMapper.assignLockerToMatchingOrders(order, normalizedLockerNumber);
        if (updated == 0) {
            throw new IllegalArgumentException("사물함 할당에 실패했습니다.");
        }

        OrderResponse updatedOrder = orderMapper.findOrderById(orderId);
        sendNotificationBestEffort(
                updatedOrder.getUserId(),
                "FitBox 픽업 안내",
                updatedOrder.getPickupPointName() + " " + normalizedLockerNumber +
                        "번 사물함에 보관되어 있습니다."
        );
        return updatedOrder;
    }

    private void validateLockerNumberInCapacity(
            Long pickupPointId,
            String receiveDate,
            String lockerNumber
    ) {
        int lockerIndex;
        try {
            lockerIndex = Integer.parseInt(lockerNumber);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("사물함 번호는 숫자로 선택해야 합니다.");
        }
        if (lockerIndex <= 0) {
            throw new IllegalArgumentException("사물함 번호는 1번 이상이어야 합니다.");
        }

        Integer totalCount = storeDailyCapacityMapper.findTotalCount(
                pickupPointId,
                LocalDate.parse(receiveDate)
        );
        if (totalCount == null || totalCount <= 0) {
            throw new IllegalArgumentException("해당 날짜의 픽업 포인트 칸 정보가 없습니다.");
        }
        if (lockerIndex > totalCount) {
            throw new IllegalArgumentException("선택한 픽업 포인트는 " + totalCount + "번 사물함까지만 사용할 수 있습니다.");
        }
    }

    @Override
    @Transactional
    public OrderResponse completeNfcPickup(
            Long orderId,
            Integer userId,
            String pickupPointName,
            String lockerNumber
    ) {
        OrderResponse order = requireOrder(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("주문 사용자 정보가 일치하지 않습니다.");
        }
        if (!RECEIVE_TYPE_PICKUP_POINT.equals(order.getReceiveType()) ||
                !"READY".equals(order.getOrderStatus())) {
            throw new IllegalArgumentException("NFC 픽업이 가능한 주문 상태가 아닙니다.");
        }

        String scannedPointName = normalizeRequired(pickupPointName, "픽업 포인트 이름");
        String scannedLockerNumber = normalizeRequired(lockerNumber, "사물함 번호");
        if (!normalizeForComparison(scannedPointName)
                .equals(normalizeForComparison(order.getPickupPointName())) ||
                !scannedLockerNumber.equals(order.getLockerNumber())) {
            throw new IllegalArgumentException("주문정보와 일치하지 않습니다.");
        }

        if (orderMapper.completeMatchingPickup(order) == 0) {
            throw new IllegalArgumentException("이미 처리된 주문입니다.");
        }
        OrderResponse updatedOrder = orderMapper.findOrderById(orderId);
        sendNotificationBestEffort(
                userId,
                "FitBox 픽업 완료",
                updatedOrder.getPickupPointName() + " " + updatedOrder.getLockerNumber() +
                        "번 사물함 상품 픽업이 완료되었습니다."
        );
        notifyStoreAdminsBestEffort(
                updatedOrder.getStoreId(),
                "FitBox 사용자 픽업 완료",
                maskCustomerName(updatedOrder.getCustomerName()) + " 고객이 " +
                        updatedOrder.getPickupPointName() + " " +
                        updatedOrder.getLockerNumber() + "번 사물함 상품을 픽업했습니다."
        );
        return updatedOrder;
    }

    private void sendNotificationBestEffort(
            Integer userId,
            String title,
            String message
    ) {
        try {
            notificationService.send(userId, title, message);
        } catch (RuntimeException error) {
            // 알림 장애가 주문 상태나 사물함 저장을 롤백시키지 않도록 분리한다.
        }
    }

    private void notifyStoreAdminsBestEffort(
            Long storeId,
            String title,
            String message
    ) {
        try {
            notificationService.notifyStoreAdmins(storeId, title, message);
        } catch (RuntimeException error) {
            // 관리자 알림 장애가 픽업 완료 처리를 되돌리지 않도록 분리한다.
        }
    }

    private String maskCustomerName(String name) {
        if (name == null || name.isBlank()) {
            return "고객";
        }
        String value = name.trim();
        if (value.length() == 1) {
            return value;
        }
        if (value.length() == 2) {
            return value.substring(0, 1) + "*";
        }
        return value.substring(0, 1) + "*" + value.substring(value.length() - 1);
    }

    private OrderResponse requireOrder(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId가 필요합니다.");
        }
        OrderResponse order = orderMapper.findOrderById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("주문을 찾을 수 없습니다.");
        }
        return order;
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "이(가) 필요합니다.");
        }
        return value.trim();
    }

    private String normalizeForComparison(String value) {
        return value == null
                ? ""
                : value.replaceAll("\\s+", " ").trim();
    }

    private void validateOrderRequest(
            Integer userId,
            String receiveType,
            Long storeId,
            Long pickupPointId,
            String address,
            String receiveDate
    ) {
        if (userId == null) {
            throw new IllegalArgumentException("userId가 필요합니다.");
        }

        if (receiveType == null || receiveType.isBlank()) {
            throw new IllegalArgumentException("receiveType이 필요합니다.");
        }

        if (!RECEIVE_TYPE_DELIVERY.equals(receiveType)
                && !RECEIVE_TYPE_PICKUP.equals(receiveType)
                && !RECEIVE_TYPE_PICKUP_POINT.equals(receiveType)) {
            throw new IllegalArgumentException("receiveType은 DELIVERY, PICKUP, PICKUP_POINT 중 하나여야 합니다.");
        }

        if (RECEIVE_TYPE_PICKUP.equals(receiveType) || RECEIVE_TYPE_PICKUP_POINT.equals(receiveType)) {
            if (storeId == null) {
                throw new IllegalArgumentException("픽업 주문은 storeId가 필요합니다.");
            }

            if (receiveDate == null || receiveDate.isBlank()) {
                throw new IllegalArgumentException("픽업 주문은 receiveDate가 필요합니다.");
            }
        }

        if (RECEIVE_TYPE_PICKUP_POINT.equals(receiveType) && pickupPointId == null) {
            throw new IllegalArgumentException("픽업 포인트 주문은 pickupPointId가 필요합니다.");
        }

        if (RECEIVE_TYPE_DELIVERY.equals(receiveType) && (address == null || address.isBlank())) {
            throw new IllegalArgumentException("배달 주문은 address가 필요합니다.");
        }
    }

    private void notifyNewStoreOrder(Long storeId, String message) {
        notificationService.notifyStoreAdmins(
                storeId,
                "FitBox 신규 주문",
                message
        );
    }

    private String newOrderMessage(String message, String receiveDate) {
        if (receiveDate == null || receiveDate.isBlank()) {
            return message;
        }
        return message + " 픽업 예정일: " + receiveDate;
    }

    private void decreasePickupCapacity(
            String receiveType,
            Long pickupPointId,
            String receiveDate,
            int count
    ) {
        if (!RECEIVE_TYPE_PICKUP_POINT.equals(receiveType)) {
            return;
        }

        if (pickupPointId == null) {
            throw new IllegalArgumentException("픽업 포인트 주문은 pickupPointId가 필요합니다.");
        }

        if (receiveDate == null || receiveDate.isBlank()) {
            throw new IllegalArgumentException("픽업 주문은 receiveDate가 필요합니다.");
        }

        if (count <= 0) {
            throw new IllegalArgumentException("차감할 픽업 수량이 올바르지 않습니다.");
        }

        LocalDate pickupDate = LocalDate.parse(receiveDate);

        int updatedCount = storeDailyCapacityMapper.decreaseRemainCount(
                pickupPointId,
                pickupDate,
                count
        );

        if (updatedCount == 0) {
            throw new IllegalStateException("선택한 날짜의 픽업 포인트 남은 칸 수가 부족합니다.");
        }
    }

    private int calculateCartTotalQuantity(List<CartItemResponseDto> items) {
        int totalQuantity = 0;

        for (CartItemResponseDto item : items) {
            totalQuantity += item.getQuantity();
        }

        return totalQuantity;
    }

    private int calculateCartProductAmount(List<CartItemResponseDto> items) {
        int totalAmount = 0;

        for (CartItemResponseDto item : items) {
            totalAmount += item.getPrice() * item.getQuantity();
        }

        return totalAmount;
    }

    private int calculateDeliveryFee(String receiveType, int productAmount) {
        if (!RECEIVE_TYPE_DELIVERY.equals(receiveType)) {
            return 0;
        }

        return productAmount >= FREE_DELIVERY_THRESHOLD ? 0 : DELIVERY_FEE;
    }

    private String getAddressIfDelivery(String receiveType, String address) {
        if (!RECEIVE_TYPE_DELIVERY.equals(receiveType)) {
            return null;
        }

        return address;
    }

    private String getPaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            return "MOCK";
        }

        return paymentMethod;
    }
}
