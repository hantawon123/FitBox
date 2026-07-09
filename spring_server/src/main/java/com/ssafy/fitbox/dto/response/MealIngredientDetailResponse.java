package com.ssafy.fitbox.dto.response;

public class MealIngredientDetailResponse {
    private Integer ingredientId;
    private String name;
    private Integer amount;
    private Double calories;

    public MealIngredientDetailResponse() {
    }

    public MealIngredientDetailResponse(
            Integer ingredientId,
            String name,
            Integer amount,
            Double calories
    ) {
        this.ingredientId = ingredientId;
        this.name = name;
        this.amount = amount;
        this.calories = calories;
    }

    public Integer getIngredientId() { return ingredientId; }
    public void setIngredientId(Integer ingredientId) { this.ingredientId = ingredientId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }
    public Double getCalories() { return calories; }
    public void setCalories(Double calories) { this.calories = calories; }
}
