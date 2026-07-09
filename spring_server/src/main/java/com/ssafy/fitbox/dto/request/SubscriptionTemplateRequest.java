package com.ssafy.fitbox.dto.request;

public class SubscriptionTemplateRequest {

    private Integer weekOfMonth;
    private Integer dayOfWeek;
    private Long mealId;
    private Integer quantity;

    public SubscriptionTemplateRequest() {
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