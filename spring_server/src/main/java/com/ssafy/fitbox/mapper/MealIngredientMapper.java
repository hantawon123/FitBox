package com.ssafy.fitbox.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.ssafy.fitbox.dto.MealIngredient;

@Mapper
public interface MealIngredientMapper {
    List<MealIngredient> selectAll();
    MealIngredient selectById(int id);
    List<MealIngredient> selectByMealId(int mealId);
    int insert(MealIngredient mealIngredient);
    int delete(int id);
    int deleteByMealId(int mealId);
}