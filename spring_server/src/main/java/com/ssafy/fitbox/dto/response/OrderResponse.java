package com.ssafy.fitbox.dto.response;

import java.time.LocalDateTime;

public class OrderResponse {

    private Long orderId;
    private Long subscriptionGroupId;
    private Integer userId;
    private String customerName;

    private Long mealId;
    private String mealName;

    private Integer quantity;
    private Integer totalPrice;

    private String receiveType;
    private String receiveDate;

    private String dateStart;
    private String dateEnd;

    private Long mon;
    private Long tue;
    private Long wed;
    private Long thu;
    private Long fri;
    private Long sat;
    private Long sun;

    private Long storeId;
    private String storeName;
    private String storeAddress;
    private Long pickupPointId;
    private String pickupPointName;
    private String pickupPointAddress;
    private String lockerNumber;

    private String address;
    private String orderStatus;

    private String paymentStatus;
    private String orderType;
    private String subscriptionStatus;

    private LocalDateTime orderTime;

    private String subscriptionItemsText;

    public OrderResponse() {
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getSubscriptionGroupId() {
        return subscriptionGroupId;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public Long getMealId() {
        return mealId;
    }

    public String getMealName() {
        return mealName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Integer getTotalPrice() {
        return totalPrice;
    }

    public String getReceiveType() {
        return receiveType;
    }

    public String getReceiveDate() {
        return receiveDate;
    }

    public String getDateStart() {
        return dateStart;
    }

    public String getDateEnd() {
        return dateEnd;
    }

    public Long getMon() {
        return mon;
    }

    public Long getTue() {
        return tue;
    }

    public Long getWed() {
        return wed;
    }

    public Long getThu() {
        return thu;
    }

    public Long getFri() {
        return fri;
    }

    public Long getSat() {
        return sat;
    }

    public Long getSun() {
        return sun;
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

    public Long getPickupPointId() {
        return pickupPointId;
    }

    public String getPickupPointName() {
        return pickupPointName;
    }

    public String getPickupPointAddress() {
        return pickupPointAddress;
    }

    public String getLockerNumber() {
        return lockerNumber;
    }

    public String getAddress() {
        return address;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public String getOrderType() {
        return orderType;
    }

    public String getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public LocalDateTime getOrderTime() {
        return orderTime;
    }

    public String getSubscriptionItemsText() {
        return subscriptionItemsText;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public void setSubscriptionGroupId(Long subscriptionGroupId) {
        this.subscriptionGroupId = subscriptionGroupId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public void setMealId(Long mealId) {
        this.mealId = mealId;
    }

    public void setMealName(String mealName) {
        this.mealName = mealName;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void setTotalPrice(Integer totalPrice) {
        this.totalPrice = totalPrice;
    }

    public void setReceiveType(String receiveType) {
        this.receiveType = receiveType;
    }

    public void setReceiveDate(String receiveDate) {
        this.receiveDate = receiveDate;
    }

    public void setDateStart(String dateStart) {
        this.dateStart = dateStart;
    }

    public void setDateEnd(String dateEnd) {
        this.dateEnd = dateEnd;
    }

    public void setMon(Long mon) {
        this.mon = mon;
    }

    public void setTue(Long tue) {
        this.tue = tue;
    }

    public void setWed(Long wed) {
        this.wed = wed;
    }

    public void setThu(Long thu) {
        this.thu = thu;
    }

    public void setFri(Long fri) {
        this.fri = fri;
    }

    public void setSat(Long sat) {
        this.sat = sat;
    }

    public void setSun(Long sun) {
        this.sun = sun;
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

    public void setPickupPointId(Long pickupPointId) {
        this.pickupPointId = pickupPointId;
    }

    public void setPickupPointName(String pickupPointName) {
        this.pickupPointName = pickupPointName;
    }

    public void setPickupPointAddress(String pickupPointAddress) {
        this.pickupPointAddress = pickupPointAddress;
    }

    public void setLockerNumber(String lockerNumber) {
        this.lockerNumber = lockerNumber;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public void setSubscriptionStatus(String subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }

    public void setOrderTime(LocalDateTime orderTime) {
        this.orderTime = orderTime;
    }

    public void setSubscriptionItemsText(String subscriptionItemsText) {
        this.subscriptionItemsText = subscriptionItemsText;
    }
}
