package com.ssafy.fitbox.dto.request;

import java.time.LocalDate;

public class SubscriptionDateMealRequest {

    private LocalDate receiveDate;
    private Long mealId;
    private Integer quantity;

    public SubscriptionDateMealRequest() {
    }

    public LocalDate getReceiveDate() {
        return receiveDate;
    }

    public Long getMealId() {
        return mealId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setReceiveDate(LocalDate receiveDate) {
        this.receiveDate = receiveDate;
    }

    public void setMealId(Long mealId) {
        this.mealId = mealId;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}