package com.ssafy.fitbox.dto.response;

public class ProductResponseDto {

    private Long id;
    private String name;
    private String mealType;
    private Integer price;
    private Double calories;
    private Double carbohydrate;
    private Double protein;
    private Double fat;
    private String imageUrl;

    // 인기 식단용 주문 수량 합계
    private Integer totalOrderCount;

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

    public Double getCalories() {
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

    public String getImageUrl() {
        return imageUrl;
    }

    public Integer getTotalOrderCount() {
        return totalOrderCount;
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

    public void setCalories(Double calories) {
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

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setTotalOrderCount(Integer totalOrderCount) {
        this.totalOrderCount = totalOrderCount;
    }
}