package com.ssafy.fitbox.service;

import com.ssafy.fitbox.dto.Meal;
import com.ssafy.fitbox.dto.MealIngredient;
import com.ssafy.fitbox.dto.Ingredient;
import com.ssafy.fitbox.dto.request.CustomMealCreateRequest;
import com.ssafy.fitbox.dto.request.CustomMealIngredientRequest;
import com.ssafy.fitbox.dto.response.CustomMealCreateResponse;
import com.ssafy.fitbox.dto.response.MealIngredientDetailResponse;
import com.ssafy.fitbox.dto.response.ProductResponseDto;
import com.ssafy.fitbox.mapper.IngredientMapper;
import com.ssafy.fitbox.mapper.MealIngredientMapper;
import com.ssafy.fitbox.mapper.MealMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class MealServiceImpl implements MealService {

    private final MealMapper mealMapper;
    private final MealIngredientMapper mealIngredientMapper;
    private final IngredientMapper ingredientMapper;

    public MealServiceImpl(
            MealMapper mealMapper,
            MealIngredientMapper mealIngredientMapper,
            IngredientMapper ingredientMapper
    ) {
        this.mealMapper = mealMapper;
        this.mealIngredientMapper = mealIngredientMapper;
        this.ingredientMapper = ingredientMapper;
    }

    @Override
    public ArrayList<Meal> selectAll() {
        return mealMapper.selectAll();
    }

    @Override
    public Meal selectById(Long id) {
        Meal meal = mealMapper.selectById(id);
        if (meal == null) {
            return null;
        }

        List<MealIngredientDetailResponse> ingredients = mealIngredientMapper
                .selectByMealId(id.intValue())
                .stream()
                .map(this::toIngredientDetail)
                .filter(detail -> detail != null)
                .toList();
        meal.setIngredients(ingredients);
        return meal;
    }

    private MealIngredientDetailResponse toIngredientDetail(MealIngredient mealIngredient) {
        Ingredient ingredient = ingredientMapper.selectById(mealIngredient.getIngredientId());
        if (ingredient == null) {
            return null;
        }
        return new MealIngredientDetailResponse(
                ingredient.getId(),
                ingredient.getName(),
                mealIngredient.getAmount(),
                ingredient.getCalories() * mealIngredient.getAmount()
        );
    }

    @Override
    public int insert(Meal meal) {
        return mealMapper.insert(meal);
    }

    @Override
    public int delete(Long id) {
        return mealMapper.delete(id);
    }

    @Override
    public List<ProductResponseDto> getProductMeals() {
        return mealMapper.findProductMeals();
    }

    @Override
    public ProductResponseDto getTodayRecommendedProduct() {
        return mealMapper.findTodayRecommendedProduct();
    }

    @Override
    public List<ProductResponseDto> getPopularMeals() {
        return mealMapper.findPopularMeals();
    }

    @Override
    public List<ProductResponseDto> getMonthlyPopularMeals() {
        return mealMapper.findMonthlyPopularMeals();
    }

    @Override
    public List<ProductResponseDto> getPopularMealsBySamePurpose(Integer userId) {
        return mealMapper.findPopularMealsBySamePurpose(userId);
    }

    @Override
    @Transactional
    public CustomMealCreateResponse createCustomMeal(
            CustomMealCreateRequest request
    ) {
        validateCustomMealCreateRequest(request);

        Meal meal = new Meal();
        meal.setName(request.getName());
        meal.setMealType("CUSTOM");
        meal.setPrice(request.getPrice());
        meal.setCalories(request.getCalories());
        meal.setCarbohydrate(request.getCarbohydrate());
        meal.setProtein(request.getProtein());
        meal.setFat(request.getFat());
        meal.setImageUrl(null);

        int mealInsertResult = mealMapper.insert(meal);

        if (mealInsertResult <= 0 || meal.getId() == null) {
            throw new IllegalStateException("커스텀 식단 저장에 실패했습니다.");
        }

        for (CustomMealIngredientRequest ingredientRequest : request.getIngredients()) {
            MealIngredient mealIngredient = new MealIngredient();
            mealIngredient.setMealId(meal.getId().intValue());
            mealIngredient.setIngredientId(ingredientRequest.getIngredientId());
            mealIngredient.setAmount(ingredientRequest.getAmount());

            int ingredientInsertResult = mealIngredientMapper.insert(mealIngredient);

            if (ingredientInsertResult <= 0) {
                throw new IllegalStateException("커스텀 식단 재료 저장에 실패했습니다.");
            }
        }

        return new CustomMealCreateResponse(
                meal.getId(),
                meal.getName(),
                meal.getPrice(),
                meal.getCalories(),
                meal.getCarbohydrate(),
                meal.getProtein(),
                meal.getFat()
        );
    }

    private void validateCustomMealCreateRequest(
            CustomMealCreateRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("커스텀 식단 정보가 없습니다.");
        }

        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("커스텀 식단 이름이 필요합니다.");
        }

        if (request.getPrice() == null || request.getPrice() < 6000) {
            throw new IllegalArgumentException("커스텀 식단은 최소 6,000원 이상 구성해야 합니다.");
        }

        if (request.getCalories() == null || request.getCalories() < 0) {
            throw new IllegalArgumentException("커스텀 식단 칼로리가 올바르지 않습니다.");
        }

        if (request.getCarbohydrate() == null
                || request.getProtein() == null
                || request.getFat() == null) {
            throw new IllegalArgumentException("커스텀 식단 영양 정보가 필요합니다.");
        }

        if (request.getIngredients() == null || request.getIngredients().isEmpty()) {
            throw new IllegalArgumentException("커스텀 식단 재료가 필요합니다.");
        }

        for (CustomMealIngredientRequest ingredient : request.getIngredients()) {
            if (ingredient.getIngredientId() == null) {
                throw new IllegalArgumentException("ingredientId가 필요합니다.");
            }

            if (ingredient.getAmount() == null || ingredient.getAmount() <= 0) {
                throw new IllegalArgumentException("재료 수량이 올바르지 않습니다.");
            }
        }
    }
}
