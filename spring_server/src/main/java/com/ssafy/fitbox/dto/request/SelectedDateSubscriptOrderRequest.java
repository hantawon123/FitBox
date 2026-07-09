package com.ssafy.fitbox.dto.request;

import java.time.LocalDate;
import java.util.List;

public class SelectedDateSubscriptOrderRequest {

    private Integer userId;
    private Long mealId;
    private List<LocalDate> receiveDates;
    private String receiveType;
    private Long storeId;
    private String address;

    public SelectedDateSubscriptOrderRequest() {
    }

    public Integer getUserId() {
        return userId;
    }

    public Long getMealId() {
        return mealId;
    }

    public List<LocalDate> getReceiveDates() {
        return receiveDates;
    }

    public String getReceiveType() {
        return receiveType;
    }

    public Long getStoreId() {
        return storeId;
    }

    public String getAddress() {
        return address;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public void setMealId(Long mealId) {
        this.mealId = mealId;
    }

    public void setReceiveDates(List<LocalDate> receiveDates) {
        this.receiveDates = receiveDates;
    }

    public void setReceiveType(String receiveType) {
        this.receiveType = receiveType;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}