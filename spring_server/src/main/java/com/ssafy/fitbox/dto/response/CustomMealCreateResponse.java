package com.ssafy.fitbox.dto.response;

public class CustomMealCreateResponse {

    private Long mealId;
    private String name;
    private Integer price;
    private Integer calories;
    private Double carbohydrate;
    private Double protein;
    private Double fat;

    public CustomMealCreateResponse() {
    }

    public CustomMealCreateResponse(
            Long mealId,
            String name,
            Integer price,
            Integer calories,
            Double carbohydrate,
            Double protein,
            Double fat
    ) {
        this.mealId = mealId;
        this.name = name;
        this.price = price;
        this.calories = calories;
        this.carbohydrate = carbohydrate;
        this.protein = protein;
        this.fat = fat;
    }

    public Long getMealId() {
        return mealId;
    }

    public String getName() {
        return name;
    }

    public Integer getPrice() {
        return price;
    }

    public Integer getCalories() {
        return calories;
    }

    public Double getCarbohydrate() {
        return carbohydrate;
    }

    public Double getProtein() {
        return protein;
    }

    public Double getFat() {
        return fat;
    }
}