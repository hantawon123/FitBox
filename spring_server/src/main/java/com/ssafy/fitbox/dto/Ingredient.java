package com.ssafy.fitbox.dto;

public class Ingredient {
    private int id;
    private String name;
    private double calories;
    private double carbohydrate;
    private double protein;
    private double fat;
    private String categories;
    private int price;
    private String imageUrl;

    public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public Ingredient() {}

    public Ingredient(int id, String name, double calories, double carbohydrate, double protein,
                      double fat, String categories, int price, String unit) {
        this.id = id;
        this.name = name;
        this.calories = calories;
        this.carbohydrate = carbohydrate;
        this.protein = protein;
        this.fat = fat;
        this.categories = categories;
        this.price = price;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getCalories() { return calories; }
    public void setCalories(double calories) { this.calories = calories; }

    public double getCarbohydrate() { return carbohydrate; }
    public void setCarbohydrate(double carbohydrate) { this.carbohydrate = carbohydrate; }

    public double getProtein() { return protein; }
    public void setProtein(double protein) { this.protein = protein; }

    public double getFat() { return fat; }
    public void setFat(double fat) { this.fat = fat; }

    public String getCategories() { return categories; }
    public void setCategories(String categories) { this.categories = categories; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

}