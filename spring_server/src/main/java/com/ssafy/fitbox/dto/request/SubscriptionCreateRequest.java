package com.ssafy.fitbox.dto.request;

import java.time.LocalDate;
import java.util.List;

public class SubscriptionCreateRequest {

    private Integer userId;
    private LocalDate subscriptionStartDate;

    private String receiveType;
    private Long storeId;
    private String address;

    private List<SubscriptionTemplateRequest> templates;

    public SubscriptionCreateRequest() {
    }

    public Integer getUserId() {
        return userId;
    }

    public LocalDate getSubscriptionStartDate() {
        return subscriptionStartDate;
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

    public List<SubscriptionTemplateRequest> getTemplates() {
        return templates;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public void setSubscriptionStartDate(LocalDate subscriptionStartDate) {
        this.subscriptionStartDate = subscriptionStartDate;
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

    public void setTemplates(List<SubscriptionTemplateRequest> templates) {
        this.templates = templates;
    }
}