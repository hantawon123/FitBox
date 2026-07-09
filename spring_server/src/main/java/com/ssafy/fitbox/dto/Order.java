package com.ssafy.fitbox.dto;

import java.time.LocalDateTime;

public class Order {

    private Long id;
    private Integer userId;
    private LocalDateTime orderTime;
    private Long menuId;
    private Integer quantity;
    private String receiveType;
    private Long storeId;
    private Long pickupPointId;
    private String lockerNumber;
    private String address;
    private String receiveDate;
    private String orderStatus;

    public String getReceiveDate() {
		return receiveDate;
	}

	public void setReceiveDate(String receiveDate) {
		this.receiveDate = receiveDate;
	}

	public Order() {
    }

    public Order(
            Long id,
            Integer userId,
            LocalDateTime orderTime,
            Long menuId,
            Integer quantity,
            String receiveType,
            Long storeId,
            Long pickupPointId,
            String address
    ) {
        this.id = id;
        this.userId = userId;
        this.orderTime = orderTime;
        this.menuId = menuId;
        this.quantity = quantity;
        this.receiveType = receiveType;
        this.storeId = storeId;
        this.pickupPointId = pickupPointId;
        this.address = address;
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

    public Long getMenuId() {
        return menuId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public String getReceiveType() {
        return receiveType;
    }

    public Long getStoreId() {
        return storeId;
    }

    public Long getPickupPointId() {
        return pickupPointId;
    }

    public String getAddress() {
        return address;
    }

    public String getLockerNumber() {
        return lockerNumber;
    }

    public String getOrderStatus() {
        return orderStatus;
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

    public void setMenuId(Long menuId) {
        this.menuId = menuId;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void setReceiveType(String receiveType) {
        this.receiveType = receiveType;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public void setPickupPointId(Long pickupPointId) {
        this.pickupPointId = pickupPointId;
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
}
