package com.ssafy.fitbox.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ssafy.fitbox.dto.MealIngredient;
import com.ssafy.fitbox.mapper.MealIngredientMapper;

@Service
public class MealIngredientServiceImpl implements MealIngredientService {

    private final MealIngredientMapper mealIngredientMapper;

    public MealIngredientServiceImpl(MealIngredientMapper mealIngredientMapper) {
        this.mealIngredientMapper = mealIngredientMapper;
    }

    @Override
    public List<MealIngredient> getMealIngredients() {
        return mealIngredientMapper.selectAll();
    }

    @Override
    public MealIngredient getMealIngredient(int id) {
        return mealIngredientMapper.selectById(id);
    }

    @Override
    public List<MealIngredient> getMealIngredientsByMealId(int mealId) {
        return mealIngredientMapper.selectByMealId(mealId);
    }

    @Override
    public boolean createMealIngredient(MealIngredient mealIngredient) {
        return mealIngredientMapper.insert(mealIngredient) == 1;
    }

    @Override
    public boolean deleteMealIngredient(int id) {
        return mealIngredientMapper.delete(id) == 1;
    }

    @Override
    public boolean deleteMealIngredientsByMealId(int mealId) {
        return mealIngredientMapper.deleteByMealId(mealId) >= 1;
    }
}