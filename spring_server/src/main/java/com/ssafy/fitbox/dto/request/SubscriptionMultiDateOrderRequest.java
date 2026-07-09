package com.ssafy.fitbox.dto.request;

import java.util.List;

public class SubscriptionMultiDateOrderRequest {

    private Integer userId;
    private String receiveType;
    private Long storeId;
    private String address;
    private List<SubscriptionDateMealRequest> items;

    public SubscriptionMultiDateOrderRequest() {
    }

    public Integer getUserId() {
        return userId;
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

    public List<SubscriptionDateMealRequest> getItems() {
        return items;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
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

    public void setItems(List<SubscriptionDateMealRequest> items) {
        this.items = items;
    }
}