package com.ssafy.fitbox.dto.request;

public class CartItemRequestDto {

    private Long mealId;
    private Integer quantity;

    public Long getMealId() {
        return mealId;
    }

    public Integer getQuantity() {
        return quantity;
    }
}