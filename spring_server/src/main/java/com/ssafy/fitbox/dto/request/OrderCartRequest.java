package com.ssafy.fitbox.dto.request;

public class OrderCartRequest {

    private Integer userId;
    private String receiveType;
    private Long storeId;
    private Long pickupPointId;
    private String address;
    private String receiveDate;
    private String paymentMethod;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getReceiveType() {
        return receiveType;
    }

    public void setReceiveType(String receiveType) {
        this.receiveType = receiveType;
    }

    public Long getStoreId() {
        return storeId;
    }

    public Long getPickupPointId() {
        return pickupPointId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public void setPickupPointId(Long pickupPointId) {
        this.pickupPointId = pickupPointId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getReceiveDate() {
        return receiveDate;
    }

    public void setReceiveDate(String receiveDate) {
        this.receiveDate = receiveDate;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
