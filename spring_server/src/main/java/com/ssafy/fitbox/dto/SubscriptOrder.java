package com.ssafy.fitbox.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class SubscriptOrder {

    private Long id;
    private Long subscriptionGroupId;
    private Integer userId;
    private LocalDateTime orderTime;
    private LocalDate dateStart;
    private LocalDate dateEnd;

    private Long mon;
    private Long tue;
    private Long wed;
    private Long thu;
    private Long fri;
    private Long sat;
    private Long sun;

    private Integer quantity;

    private String receiveType;
    private Long storeId;
    private String address;

    public SubscriptOrder() {
    }

    public Long getId() {
        return id;
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

    public LocalDate getDateStart() {
        return dateStart;
    }

    public LocalDate getDateEnd() {
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

    public Integer getQuantity() {
        return quantity;
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

    public void setSubscriptionGroupId(Long subscriptionGroupId) {
        this.subscriptionGroupId = subscriptionGroupId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public void setOrderTime(LocalDateTime orderTime) {
        this.orderTime = orderTime;
    }

    public void setDateStart(LocalDate dateStart) {
        this.dateStart = dateStart;
    }

    public void setDateEnd(LocalDate dateEnd) {
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

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
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