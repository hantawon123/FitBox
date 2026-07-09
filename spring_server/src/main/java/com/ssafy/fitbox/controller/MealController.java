package com.ssafy.fitbox.controller;

import com.ssafy.fitbox.dto.Meal;
import com.ssafy.fitbox.dto.request.CustomMealCreateRequest;
import com.ssafy.fitbox.dto.response.CustomMealCreateResponse;
import com.ssafy.fitbox.dto.response.ProductResponseDto;
import com.ssafy.fitbox.service.MealService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/meals")
@Tag(name = "Meal", description = "식단 API")
public class MealController {

    private final MealService mealService;

    public MealController(MealService mealService) {
        this.mealService = mealService;
    }

    @Operation(
            summary = "같은 목적 사용자 인기 식단 조회",
            description = "로그인한 사용자와 같은 purpose를 가진 사용자들이 많이 주문한 식단을 조회합니다."
    )
    @GetMapping("/popular/purpose/{userId}")
    public ResponseEntity<List<ProductResponseDto>> getPopularMealsBySamePurpose(
            @PathVariable Integer userId
    ) {
        return ResponseEntity.ok(mealService.getPopularMealsBySamePurpose(userId));
    }
    
    @Operation(
            summary = "전체 식단 조회",
            description = "meal_table에 저장된 전체 식단을 조회합니다."
    )
    @GetMapping
    public ResponseEntity<ArrayList<Meal>> selectAll() {
        return ResponseEntity.ok(mealService.selectAll());
    }

    @Operation(
            summary = "식단 상세 조회",
            description = "meal id를 기준으로 식단 상세 정보를 조회합니다."
    )
    @GetMapping("/{id}")
    public ResponseEntity<Meal> selectById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(mealService.selectById(id));
    }

    @Operation(
            summary = "식단 등록",
            description = "meal_table에 식단을 등록합니다. mealType이 없으면 기본 PRODUCT로 저장합니다."
    )
    @PostMapping
    public ResponseEntity<Integer> insert(
            @RequestBody Meal meal
    ) {
        return ResponseEntity.ok(mealService.insert(meal));
    }

    @Operation(
            summary = "식단 삭제",
            description = "meal id를 기준으로 식단을 삭제합니다."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Integer> delete(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(mealService.delete(id));
    }

    @Operation(
            summary = "완제품 식단 목록 조회",
            description = "meal_table에서 meal_type이 PRODUCT인 완제품 식단 목록을 조회합니다."
    )
    @GetMapping("/products")
    public ResponseEntity<List<ProductResponseDto>> getProductMeals() {
        return ResponseEntity.ok(mealService.getProductMeals());
    }

    @Operation(
            summary = "오늘의 추천 완제품 식단 조회",
            description = "완제품 식단 중 오늘 날짜 기준으로 추천 식단 1개를 조회합니다."
    )
    @GetMapping("/recommend/today")
    public ResponseEntity<ProductResponseDto> getTodayRecommendedProduct() {
        return ResponseEntity.ok(mealService.getTodayRecommendedProduct());
    }

    @Operation(
            summary = "인기 식단 조회",
            description = "다른 사용자들이 많이 주문한 식단을 주문 수량 기준으로 조회합니다."
    )
    @GetMapping("/popular")
    public ResponseEntity<List<ProductResponseDto>> getPopularMeals() {
        return ResponseEntity.ok(mealService.getPopularMeals());
    }

    @Operation(
            summary = "이번 달 인기 식단 조회",
            description = "이번 달 1일 00시부터 현재까지 실제 주문 수량을 합산해 인기 식단을 조회합니다."
    )
    @GetMapping("/popular/monthly")
    public ResponseEntity<List<ProductResponseDto>> getMonthlyPopularMeals() {
        return ResponseEntity.ok(mealService.getMonthlyPopularMeals());
    }
    
    @Operation(
            summary = "커스텀 식단 생성",
            description = "사용자가 선택한 재료를 기반으로 meal_table에는 CUSTOM 식단을 저장하고, meal_ingredient_table에는 재료 구성을 저장합니다."
    )
    @PostMapping("/custom")
    public ResponseEntity<?> createCustomMeal(
            @RequestBody CustomMealCreateRequest request
    ) {
        try {
            CustomMealCreateResponse response = mealService.createCustomMeal(request);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("커스텀 식단 생성에 실패했습니다: " + e.getMessage());
        }
    }
}
