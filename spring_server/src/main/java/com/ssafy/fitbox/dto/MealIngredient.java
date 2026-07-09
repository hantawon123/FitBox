package com.ssafy.fitbox.dto;

public class MealIngredient {
    private int id;
    private int mealId;
    private int ingredientId;
    private int amount;

    public MealIngredient() {}

    public MealIngredient(int id, int mealId, int ingredientId, int amount) {
        this.id = id;
        this.mealId = mealId;
        this.ingredientId = ingredientId;
        this.amount = amount;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMealId() { return mealId; }
    public void setMealId(int mealId) { this.mealId = mealId; }

    public int getIngredientId() { return ingredientId; }
    public void setIngredientId(int ingredientId) { this.ingredientId = ingredientId; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
}