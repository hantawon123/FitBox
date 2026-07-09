package com.ssafy.fitbox.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ssafy.fitbox.dto.MealIngredient;
import com.ssafy.fitbox.service.MealIngredientService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "MealIngredient API", description = "식단과 식재료 연결 정보 CRD API")
@RestController
@CrossOrigin("*")
@RequestMapping("/meal-ingredients")
public class MealIngredientController {

    private final MealIngredientService mealIngredientService;

    public MealIngredientController(MealIngredientService mealIngredientService) {
        this.mealIngredientService = mealIngredientService;
    }

    @Operation(
            summary = "식단-식재료 연결 전체 조회",
            description = "meal_ingredient_table에 저장된 모든 식단-식재료 연결 정보를 조회합니다."
    )
    @GetMapping
    public ResponseEntity<List<MealIngredient>> getMealIngredients() {
        return ResponseEntity.ok(mealIngredientService.getMealIngredients());
    }

    @Operation(
            summary = "식단-식재료 연결 단건 조회",
            description = "기본키 id를 기준으로 식단-식재료 연결 정보를 조회합니다."
    )
    @GetMapping("/{id}")
    public ResponseEntity<MealIngredient> getMealIngredient(
            @Parameter(description = "식단-식재료 연결 기본키 id", example = "1")
            @PathVariable int id
    ) {
        MealIngredient mealIngredient = mealIngredientService.getMealIngredient(id);

        if (mealIngredient == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(mealIngredient);
    }

    @Operation(
            summary = "특정 식단의 식재료 목록 조회",
            description = "meal_id를 기준으로 해당 식단에 포함된 식재료 연결 목록을 조회합니다."
    )
    @GetMapping("/meal/{mealId}")
    public ResponseEntity<List<MealIngredient>> getMealIngredientsByMealId(
            @Parameter(description = "식단 id", example = "1")
            @PathVariable int mealId
    ) {
        return ResponseEntity.ok(mealIngredientService.getMealIngredientsByMealId(mealId));
    }

    @Operation(
            summary = "식단-식재료 연결 등록",
            description = "특정 식단에 식재료와 수량 정보를 연결하여 등록합니다. Update는 제공하지 않으며, 수정이 필요하면 삭제 후 다시 등록합니다."
    )
    @PostMapping
    public ResponseEntity<String> createMealIngredient(@RequestBody MealIngredient mealIngredient) {
        boolean result = mealIngredientService.createMealIngredient(mealIngredient);

        if (result) {
            return ResponseEntity.ok("식단 재료 등록 성공");
        }

        return ResponseEntity.badRequest().body("식단 재료 등록 실패");
    }

    @Operation(
            summary = "식단-식재료 연결 단건 삭제",
            description = "기본키 id를 기준으로 식단-식재료 연결 정보를 삭제합니다."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteMealIngredient(
            @Parameter(description = "삭제할 식단-식재료 연결 기본키 id", example = "1")
            @PathVariable int id
    ) {
        boolean result = mealIngredientService.deleteMealIngredient(id);

        if (result) {
            return ResponseEntity.ok("식단 재료 삭제 성공");
        }

        return ResponseEntity.badRequest().body("식단 재료 삭제 실패");
    }

    @Operation(
            summary = "특정 식단의 식재료 연결 전체 삭제",
            description = "meal_id를 기준으로 해당 식단에 연결된 모든 식재료 정보를 삭제합니다. 식단 재구성 시 사용합니다."
    )
    @DeleteMapping("/meal/{mealId}")
    public ResponseEntity<String> deleteMealIngredientsByMealId(
            @Parameter(description = "식단 id", example = "1")
            @PathVariable int mealId
    ) {
        boolean result = mealIngredientService.deleteMealIngredientsByMealId(mealId);

        if (result) {
            return ResponseEntity.ok("해당 식단의 재료 전체 삭제 성공");
        }

        return ResponseEntity.badRequest().body("해당 식단의 재료 전체 삭제 실패");
    }
}