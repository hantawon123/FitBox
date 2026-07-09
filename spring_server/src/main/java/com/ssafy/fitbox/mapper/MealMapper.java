package com.ssafy.fitbox.mapper;

import com.ssafy.fitbox.dto.Meal;
import com.ssafy.fitbox.dto.response.ProductResponseDto;

import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.List;

@Mapper
public interface MealMapper {

    ArrayList<Meal> selectAll();

    Meal selectById(Long id);

    int insert(Meal meal);

    int delete(Long id);

    List<ProductResponseDto> findProductMeals();

    ProductResponseDto findTodayRecommendedProduct();

    List<ProductResponseDto> findPopularMeals();

    List<ProductResponseDto> findMonthlyPopularMeals();
    
    List<ProductResponseDto> findPopularMealsBySamePurpose(Integer userId);
}
