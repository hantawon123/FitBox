package com.ssafy.fitbox.service;

import java.util.List;

import com.ssafy.fitbox.dto.MealIngredient;

public interface MealIngredientService {

    List<MealIngredient> getMealIngredients();

    MealIngredient getMealIngredient(int id);

    List<MealIngredient> getMealIngredientsByMealId(int mealId);

    boolean createMealIngredient(MealIngredient mealIngredient);

    boolean deleteMealIngredient(int id);

    boolean deleteMealIngredientsByMealId(int mealId);
}