package com.ssafy.fitbox.dto.response;

public class SubscriptionTemplateResponse {

    private Long templateId;
    private Integer weekOfMonth;
    private Integer dayOfWeek;
    private String dayOfWeekText;

    private Long mealId;
    private String mealName;
    private Integer mealPrice;
    private Integer quantity;

    public SubscriptionTemplateResponse() {
    }

    public Long getTemplateId() {
        return templateId;
    }

    public Integer getWeekOfMonth() {
        return weekOfMonth;
    }

    public Integer getDayOfWeek() {
        return dayOfWeek;
    }

    public String getDayOfWeekText() {
        return dayOfWeekText;
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

    public Integer getMealPrice() {
        return mealPrice;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public void setWeekOfMonth(Integer weekOfMonth) {
        this.weekOfMonth = weekOfMonth;
    }

    public void setDayOfWeek(Integer dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public void setDayOfWeekText(String dayOfWeekText) {
        this.dayOfWeekText = dayOfWeekText;
    }

    public void setMealId(Long mealId) {
        this.mealId = mealId;
    }

    public void setMealName(String mealName) {
        this.mealName = mealName;
    }

    public void setMealPrice(Integer mealPrice) {
        this.mealPrice = mealPrice;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
