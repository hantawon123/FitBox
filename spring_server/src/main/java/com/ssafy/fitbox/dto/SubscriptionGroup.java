package com.ssafy.fitbox.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class SubscriptionGroup {

    private Long id;
    private Integer userId;
    private LocalDateTime orderTime;

    private LocalDate subscriptionStartDate;
    private LocalDate subscriptionEndDate;
    private LocalDate nextOrderMonth;

    private String status;

    private String receiveType;
    private Long storeId;
    private String address;

    public SubscriptionGroup() {
    }

    public Long getId() {
        return id;
    }

    public Integer getUserId() {
        return userId;
    }

    public LocalDateTime getOrderTime() {
        return orderTime;
    }

    public LocalDate getSubscriptionStartDate() {
        return subscriptionStartDate;
    }

    public LocalDate getSubscriptionEndDate() {
        return subscriptionEndDate;
    }

    public LocalDate getNextOrderMonth() {
        return nextOrderMonth;
    }

    public String getStatus() {
        return status;
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

    public void setId(Long id) {
        this.id = id;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public void setOrderTime(LocalDateTime orderTime) {
        this.orderTime = orderTime;
    }

    public void setSubscriptionStartDate(LocalDate subscriptionStartDate) {
        this.subscriptionStartDate = subscriptionStartDate;
    }

    public void setSubscriptionEndDate(LocalDate subscriptionEndDate) {
        this.subscriptionEndDate = subscriptionEndDate;
    }

    public void setNextOrderMonth(LocalDate nextOrderMonth) {
        this.nextOrderMonth = nextOrderMonth;
    }

    public void setStatus(String status) {
        this.status = status;
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