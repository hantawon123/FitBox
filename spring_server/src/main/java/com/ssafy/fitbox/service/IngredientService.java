package com.ssafy.fitbox.service;

import java.util.List;

import com.ssafy.fitbox.dto.Ingredient;

public interface IngredientService {

    List<Ingredient> getIngredients();

    Ingredient getIngredient(int id);

    boolean createIngredient(Ingredient ingredient);

    boolean updateIngredient(Ingredient ingredient);

    boolean deleteIngredient(int id);
}