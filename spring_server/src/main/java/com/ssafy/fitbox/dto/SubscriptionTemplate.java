package com.ssafy.fitbox.dto;

public class SubscriptionTemplate {

    private Long id;
    private Long subscriptionGroupId;
    private Integer weekOfMonth;
    private Integer dayOfWeek;
    private Long mealId;
    private Integer quantity;

    public SubscriptionTemplate() {
    }

    public Long getId() {
        return id;
    }

    public Long getSubscriptionGroupId() {
        return subscriptionGroupId;
    }

    public Integer getWeekOfMonth() {
        return weekOfMonth;
    }

    public Integer getDayOfWeek() {
        return dayOfWeek;
    }

    public Long getMealId() {
        return mealId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setSubscriptionGroupId(Long subscriptionGroupId) {
        this.subscriptionGroupId = subscriptionGroupId;
    }

    public void setWeekOfMonth(Integer weekOfMonth) {
        this.weekOfMonth = weekOfMonth;
    }

    public void setDayOfWeek(Integer dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public void setMealId(Long mealId) {
        this.mealId = mealId;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}