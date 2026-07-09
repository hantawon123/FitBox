package com.ssafy.fitbox.dto;

import com.ssafy.fitbox.dto.response.MealIngredientDetailResponse;
import java.util.ArrayList;
import java.util.List;

public class Meal {

    private Long id;
    private String name;
    private String mealType;
    private Integer price;
    private Integer calories;
    private Double carbohydrate;
    private Double protein;
    private Double fat;
    private String imageUrl;
    private List<MealIngredientDetailResponse> ingredients = new ArrayList<>();

    public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

    public List<MealIngredientDetailResponse> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<MealIngredientDetailResponse> ingredients) {
        this.ingredients = ingredients;
    }

	public Meal() {
    }

    public Meal(
            Long id,
            String name,
            String mealType,
            Integer price,
            Integer calories,
            Double carbohydrate,
            Double protein,
            Double fat
    ) {
        this.id = id;
        this.name = name;
        this.mealType = mealType;
        this.price = price;
        this.calories = calories;
        this.carbohydrate = carbohydrate;
        this.protein = protein;
        this.fat = fat;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getMealType() {
        return mealType;
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

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
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
}
