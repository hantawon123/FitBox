package com.ssafy.fitbox.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ssafy.fitbox.dto.SubscriptOrder;
import com.ssafy.fitbox.dto.request.SelectedDateSubscriptOrderRequest;
import com.ssafy.fitbox.dto.request.SubscriptionCreateRequest;
import com.ssafy.fitbox.dto.request.SubscriptionMultiDateOrderRequest;
import com.ssafy.fitbox.dto.response.SubscriptionResponse;
import com.ssafy.fitbox.service.SubscriptOrderService;

@RestController
@CrossOrigin("*")
@RequestMapping("/subscriptorderapi")
public class SubscriptOrderRestController {

    private final SubscriptOrderService subscriptOrderService;

    public SubscriptOrderRestController(SubscriptOrderService subscriptOrderService) {
        this.subscriptOrderService = subscriptOrderService;
    }

    @GetMapping("/order")
    public ResponseEntity<?> selectAll() {
        ArrayList<SubscriptOrder> list = subscriptOrderService.selectAll();

        if (list == null || list.isEmpty()) {
            return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<ArrayList<SubscriptOrder>>(list, HttpStatus.OK);
    }

    @GetMapping("/order/{id}")
    public ResponseEntity<?> selectById(@PathVariable("id") Long id) {
        SubscriptOrder order = subscriptOrderService.selectById(id);

        if (order == null) {
            return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<SubscriptOrder>(order, HttpStatus.OK);
    }

    @PostMapping("/order")
    public ResponseEntity<?> insert(@RequestBody SubscriptOrder order) {
        try {
            validateSubscriptOrder(order);

            int result = subscriptOrderService.insert(order);

            if (result > 0) {
                return new ResponseEntity<SubscriptOrder>(order, HttpStatus.CREATED);
            }

            return new ResponseEntity<String>(
                    "구독 주문 생성에 실패했습니다.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<String>(
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    @PutMapping("/order/{id}")
    public ResponseEntity<?> update(
            @PathVariable("id") Long id,
            @RequestBody SubscriptOrder order
    ) {
        try {
            order.setId(id);
            validateSubscriptOrder(order);

            int result = subscriptOrderService.update(order);

            if (result > 0) {
                return new ResponseEntity<String>("success", HttpStatus.OK);
            }

            return new ResponseEntity<String>("fail", HttpStatus.INTERNAL_SERVER_ERROR);

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<String>(
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    @DeleteMapping("/order/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id) {
        int result = subscriptOrderService.delete(id);

        if (result > 0) {
            return new ResponseEntity<String>("success", HttpStatus.OK);
        }

        return new ResponseEntity<String>("fail", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @PostMapping("/order/selected-dates")
    public ResponseEntity<?> insertSelectedDateOrders(
            @RequestBody SelectedDateSubscriptOrderRequest request
    ) {
        try {
            List<SubscriptOrder> orders =
                    subscriptOrderService.insertSelectedDateOrders(request);

            return new ResponseEntity<List<SubscriptOrder>>(
                    orders,
                    HttpStatus.CREATED
            );

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<String>(
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );

        } catch (Exception e) {
            return new ResponseEntity<String>(
                    "선택 날짜 구독 주문 생성에 실패했습니다: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @PostMapping("/order/multi-date")
    public ResponseEntity<?> insertMultiDateOrders(
            @RequestBody SubscriptionMultiDateOrderRequest request
    ) {
        try {
            List<SubscriptOrder> orders =
                    subscriptOrderService.insertMultiDateOrders(request);

            return new ResponseEntity<List<SubscriptOrder>>(
                    orders,
                    HttpStatus.CREATED
            );

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<String>(
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );

        } catch (Exception e) {
            return new ResponseEntity<String>(
                    "다중 날짜 구독 주문 생성에 실패했습니다: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @PostMapping("/subscriptions")
    public ResponseEntity<?> createMonthlySubscription(
            @RequestBody SubscriptionCreateRequest request
    ) {
        try {
            SubscriptionResponse response =
                    subscriptOrderService.createMonthlySubscription(request);

            return new ResponseEntity<SubscriptionResponse>(
                    response,
                    HttpStatus.CREATED
            );

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<String>(
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );

        } catch (Exception e) {
            return new ResponseEntity<String>(
                    "정기 구독 생성에 실패했습니다: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @GetMapping("/subscriptions/users/{userId}")
    public ResponseEntity<?> getUserSubscriptions(
            @PathVariable("userId") Integer userId
    ) {
        try {
            List<SubscriptionResponse> response =
                    subscriptOrderService.getUserSubscriptions(userId);

            if (response == null || response.isEmpty()) {
                return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
            }

            return new ResponseEntity<List<SubscriptionResponse>>(
                    response,
                    HttpStatus.OK
            );

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<String>(
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    @PatchMapping("/subscriptions/{subscriptionGroupId}/cancel")
    public ResponseEntity<?> cancelSubscription(
            @PathVariable Long subscriptionGroupId,
            @RequestParam Integer userId
    ) {
        try {
            subscriptOrderService.cancelSubscription(subscriptionGroupId, userId);
            return ResponseEntity.ok("구독이 취소되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private void validateSubscriptOrder(SubscriptOrder order) {
        if (order == null) {
            throw new IllegalArgumentException("구독 주문 정보가 없습니다.");
        }

        if (order.getUserId() == null) {
            throw new IllegalArgumentException("userId가 필요합니다.");
        }

        if (order.getDateStart() == null) {
            throw new IllegalArgumentException("구독 시작일이 필요합니다.");
        }

        if (order.getDateEnd() == null) {
            throw new IllegalArgumentException("구독 종료일이 필요합니다.");
        }

        if (order.getDateEnd().isBefore(order.getDateStart())) {
            throw new IllegalArgumentException("구독 종료일은 시작일보다 빠를 수 없습니다.");
        }

        if (order.getReceiveType() == null || order.getReceiveType().isBlank()) {
            throw new IllegalArgumentException("receiveType이 필요합니다.");
        }

        if (!order.getReceiveType().equals("DELIVERY")) {
            throw new IllegalArgumentException("구독은 DELIVERY만 지원합니다.");
        }

        if (order.getReceiveType().equals("DELIVERY")
                && (order.getAddress() == null || order.getAddress().isBlank())) {
            throw new IllegalArgumentException("배달 구독은 address가 필요합니다.");
        }

        if (!hasAnySelectedDay(order)) {
            throw new IllegalArgumentException("수령 요일을 하나 이상 선택해야 합니다.");
        }

        if (order.getQuantity() != null && order.getQuantity() <= 0) {
            throw new IllegalArgumentException("수량은 1개 이상이어야 합니다.");
        }
    }

    private boolean hasAnySelectedDay(SubscriptOrder order) {
        return order.getMon() != null
                || order.getTue() != null
                || order.getWed() != null
                || order.getThu() != null
                || order.getFri() != null
                || order.getSat() != null
                || order.getSun() != null;
    }
}
