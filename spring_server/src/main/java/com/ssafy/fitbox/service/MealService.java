package com.ssafy.fitbox.service;

import com.ssafy.fitbox.dto.Meal;
import com.ssafy.fitbox.dto.request.CustomMealCreateRequest;
import com.ssafy.fitbox.dto.response.CustomMealCreateResponse;
import com.ssafy.fitbox.dto.response.ProductResponseDto;

import java.util.ArrayList;
import java.util.List;

public interface MealService {

    ArrayList<Meal> selectAll();

    Meal selectById(Long id);

    int insert(Meal meal);

    int delete(Long id);

    List<ProductResponseDto> getProductMeals();

    ProductResponseDto getTodayRecommendedProduct();

    List<ProductResponseDto> getPopularMeals();

    List<ProductResponseDto> getMonthlyPopularMeals();
    
    List<ProductResponseDto> getPopularMealsBySamePurpose(Integer userId);

    CustomMealCreateResponse createCustomMeal(CustomMealCreateRequest request);
}
