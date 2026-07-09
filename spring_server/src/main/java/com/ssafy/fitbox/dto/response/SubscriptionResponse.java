package com.ssafy.fitbox.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class SubscriptionResponse {

    private Long subscriptionGroupId;
    private Integer userId;
    private LocalDateTime orderTime;

    private LocalDate subscriptionStartDate;
    private LocalDate subscriptionEndDate;
    private LocalDate nextOrderMonth;

    private String status;

    private String receiveType;
    private Long storeId;
    private String storeName;
    private String storeAddress;
    private String address;

    private List<SubscriptionTemplateResponse> templates;

    public SubscriptionResponse() {
    }

    public Long getSubscriptionGroupId() {
        return subscriptionGroupId;
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

    public String getStoreName() {
        return storeName;
    }

    public String getStoreAddress() {
        return storeAddress;
    }

    public String getAddress() {
        return address;
    }

    public List<SubscriptionTemplateResponse> getTemplates() {
        return templates;
    }

    public void setSubscriptionGroupId(Long subscriptionGroupId) {
        this.subscriptionGroupId = subscriptionGroupId;
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

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public void setStoreAddress(String storeAddress) {
        this.storeAddress = storeAddress;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setTemplates(List<SubscriptionTemplateResponse> templates) {
        this.templates = templates;
    }
}