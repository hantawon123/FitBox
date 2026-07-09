package com.ssafy.fitbox.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ssafy.fitbox.dto.Ingredient;
import com.ssafy.fitbox.mapper.IngredientMapper;

@Service
public class IngredientServiceImpl implements IngredientService {

    private final IngredientMapper ingredientMapper;

    public IngredientServiceImpl(IngredientMapper ingredientMapper) {
        this.ingredientMapper = ingredientMapper;
    }

    @Override
    public List<Ingredient> getIngredients() {
        return ingredientMapper.selectAll();
    }

    @Override
    public Ingredient getIngredient(int id) {
        return ingredientMapper.selectById(id);
    }

    @Override
    public boolean createIngredient(Ingredient ingredient) {
        return ingredientMapper.insert(ingredient) == 1;
    }

    @Override
    public boolean updateIngredient(Ingredient ingredient) {
        return ingredientMapper.update(ingredient) == 1;
    }

    @Override
    public boolean deleteIngredient(int id) {
        return ingredientMapper.delete(id) == 1;
    }
}