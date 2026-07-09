package com.ssafy.fitbox.dto.request;

import java.util.List;

public class CustomMealCreateRequest {

    private String name;
    private Integer price;
    private Integer calories;
    private Double carbohydrate;
    private Double protein;
    private Double fat;
    private List<CustomMealIngredientRequest> ingredients;

    public CustomMealCreateRequest() {
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

    public List<CustomMealIngredientRequest> getIngredients() {
        return ingredients;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public void setCalories(Integer calories) {
        this.calories = calories;
    }

    public void setCarbohydrate(Double carbohydrate) {
        this.carbohydrate = carbohydrate;
    }

    public void setProtein(Double protein) {
        this.protein = protein;
    }

    public void setFat(Double fat) {
        this.fat = fat;
    }

    public void setIngredients(List<CustomMealIngredientRequest> ingredients) {
        this.ingredients = ingredients;
    }
}