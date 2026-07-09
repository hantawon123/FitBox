package com.ssafy.fitbox.dto.request;

public class CustomMealIngredientRequest {

    private Integer ingredientId;
    private Integer amount;

    public CustomMealIngredientRequest() {
    }

    public Integer getIngredientId() {
        return ingredientId;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setIngredientId(Integer ingredientId) {
        this.ingredientId = ingredientId;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }
}