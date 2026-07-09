package com.ssafy.fitbox.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.fitbox.dto.Order;
import com.ssafy.fitbox.dto.request.DirectOrderRequest;
import com.ssafy.fitbox.dto.request.OrderCartRequest;
import com.ssafy.fitbox.dto.request.OrderStatusUpdateRequest;
import com.ssafy.fitbox.dto.request.LockerAssignmentRequest;
import com.ssafy.fitbox.dto.request.NfcPickupRequest;
import com.ssafy.fitbox.dto.response.OrderResponse;
import com.ssafy.fitbox.service.OrderService;

@RestController
@CrossOrigin("*")
@RequestMapping("/orderapi")
public class OrderRestController {

    @Autowired
    OrderService orderService;

    /*
     * 기존 Order 기본 CRUD API
     */

    @GetMapping("/order")
    public ResponseEntity<?> selectAll() {
        ArrayList<Order> list = orderService.selectAll();

        if (list == null || list.isEmpty()) {
            return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<ArrayList<Order>>(list, HttpStatus.OK);
    }

    @GetMapping("/order/{id}")
    public ResponseEntity<?> selectById(@PathVariable("id") Long id) {
        Order order = orderService.selectById(id);

        if (order == null) {
            return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<Order>(order, HttpStatus.OK);
    }

    @PostMapping("/order")
    public ResponseEntity<?> insert(@RequestBody Order order) {
        int result = orderService.insert(order);

        if (result > 0) {
            return new ResponseEntity<Order>(order, HttpStatus.CREATED);
        }

        return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @PutMapping("/order/{id}")
    public ResponseEntity<?> update(
            @PathVariable("id") Long id,
            @RequestBody Order order
    ) {
        order.setId(id);

        int result = orderService.update(order);

        if (result > 0) {
            return new ResponseEntity<String>("success", HttpStatus.OK);
        }

        return new ResponseEntity<String>("fail", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @DeleteMapping("/order/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id) {
        int result = orderService.delete(id);

        if (result > 0) {
            return new ResponseEntity<String>("success", HttpStatus.OK);
        }

        return new ResponseEntity<String>("fail", HttpStatus.INTERNAL_SERVER_ERROR);
    }


    /*
     * 새로 추가하는 주문 기능 API
     */

    // 장바구니 전체 주문
    @PostMapping("/orders/cart")
    public ResponseEntity<?> orderFromCart(
            @RequestBody OrderCartRequest request
    ) {
        try {
            List<OrderResponse> response = orderService.orderFromCart(request);

            if (response == null || response.isEmpty()) {
                return new ResponseEntity<String>(
                        "장바구니가 비어 있습니다.",
                        HttpStatus.BAD_REQUEST
                );
            }

            return new ResponseEntity<List<OrderResponse>>(response, HttpStatus.CREATED);

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<String>(
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );

        } catch (Exception e) {
            return new ResponseEntity<String>(
                    "장바구니 주문 처리 중 오류가 발생했습니다.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    // 상품 상세에서 바로 주문
    @PostMapping("/orders/direct")
    public ResponseEntity<?> orderDirect(
            @RequestBody DirectOrderRequest request
    ) {
        try {
            OrderResponse response = orderService.orderDirect(request);

            if (response == null) {
                return new ResponseEntity<String>(
                        "주문 생성에 실패했습니다.",
                        HttpStatus.INTERNAL_SERVER_ERROR
                );
            }

            return new ResponseEntity<OrderResponse>(response, HttpStatus.CREATED);

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<String>(
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );

        } catch (Exception e) {
            return new ResponseEntity<String>(
                    "바로 주문 처리 중 오류가 발생했습니다.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    // 주문 상세 조회
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<?> getOrder(
            @PathVariable("orderId") Long orderId
    ) {
        OrderResponse response = orderService.getOrder(orderId);

        if (response == null) {
            return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<OrderResponse>(response, HttpStatus.OK);
    }

    // 특정 회원의 주문 목록 조회
    @GetMapping("/orders/users/{userId}")
    public ResponseEntity<?> getUserOrders(
            @PathVariable("userId") Integer userId
    ) {
        List<OrderResponse> response = orderService.getUserOrders(userId);

        if (response == null || response.isEmpty()) {
            return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<List<OrderResponse>>(response, HttpStatus.OK);
    }

    @GetMapping("/admin/orders")
    public ResponseEntity<List<OrderResponse>> getAdminOrders() {
        return ResponseEntity.ok(orderService.getAdminOrders());
    }

    @PatchMapping("/orders/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable("orderId") Long orderId,
            @RequestBody OrderStatusUpdateRequest request
    ) {
        try {
            OrderResponse response = orderService.updateOrderStatus(
                    orderId,
                    request.getOrderStatus()
            );

            return new ResponseEntity<OrderResponse>(response, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<String>(
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );

        } catch (Exception e) {
            return new ResponseEntity<String>(
                    "주문 상태 변경 중 오류가 발생했습니다.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @PatchMapping("/orders/{orderId}/locker")
    public ResponseEntity<?> assignLocker(
            @PathVariable Long orderId,
            @RequestBody LockerAssignmentRequest request
    ) {
        try {
            return ResponseEntity.ok(
                    orderService.assignLocker(orderId, request.getLockerNumber())
            );
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(error.getMessage());
        } catch (Exception error) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("사물함 할당 중 오류가 발생했습니다: " + error.getMessage());
        }
    }

    @PostMapping("/orders/{orderId}/nfc-pickup")
    public ResponseEntity<?> completeNfcPickup(
            @PathVariable Long orderId,
            @RequestBody NfcPickupRequest request
    ) {
        try {
            return ResponseEntity.ok(
                    orderService.completeNfcPickup(
                            orderId,
                            request.getUserId(),
                            request.getPickupPointName(),
                            request.getLockerNumber()
                    )
            );
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(error.getMessage());
        }
    }
}
