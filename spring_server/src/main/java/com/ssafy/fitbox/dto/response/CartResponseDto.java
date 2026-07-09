package com.ssafy.fitbox.dto.response;

import java.util.List;

public class CartResponseDto {

    private Long cartId;
    private Integer userId;
    private List<CartItemResponseDto> items;
    private Integer totalPrice;

    public CartResponseDto(
            Long cartId,
            Integer userId,
            List<CartItemResponseDto> items
    ) {
        this.cartId = cartId;
        this.userId = userId;
        this.items = items;
        this.totalPrice = calculateTotalPrice(items);
    }

    private Integer calculateTotalPrice(List<CartItemResponseDto> items) {
        return items.stream()
                .mapToInt(item -> {
                    int price = item.getPrice() == null ? 0 : item.getPrice();
                    int quantity = item.getQuantity() == null ? 1 : item.getQuantity();
                    return price * quantity;
                })
                .sum();
    }

    public Long getCartId() {
        return cartId;
    }

    public Integer getUserId() {
        return userId;
    }

    public List<CartItemResponseDto> getItems() {
        return items;
    }

    public Integer getTotalPrice() {
        return totalPrice;
    }
}