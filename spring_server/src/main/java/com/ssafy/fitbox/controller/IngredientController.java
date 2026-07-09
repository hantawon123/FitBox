package com.ssafy.fitbox.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ssafy.fitbox.dto.Ingredient;
import com.ssafy.fitbox.service.IngredientService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Ingredient API", description = "식재료 정보 CRUD API")
@RestController
@CrossOrigin("*")
@RequestMapping("/ingredients")
public class IngredientController {

    private final IngredientService ingredientService;

    public IngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @Operation(summary = "식재료 전체 조회", description = "ingredient_table에 저장된 모든 식재료 정보를 조회합니다.")
    @GetMapping
    public ResponseEntity<List<Ingredient>> getIngredients() {
        return ResponseEntity.ok(ingredientService.getIngredients());
    }

    @Operation(summary = "식재료 단건 조회", description = "기본키 id를 기준으로 식재료 하나를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<Ingredient> getIngredient(
            @Parameter(description = "식재료 기본키 id", example = "1")
            @PathVariable int id
    ) {
        Ingredient ingredient = ingredientService.getIngredient(id);

        if (ingredient == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(ingredient);
    }

    @Operation(summary = "식재료 등록", description = "새로운 식재료 정보를 ingredient_table에 등록합니다.")
    @PostMapping
    public ResponseEntity<String> createIngredient(@RequestBody Ingredient ingredient) {
        boolean result = ingredientService.createIngredient(ingredient);

        if (result) {
            return ResponseEntity.ok("재료 등록 성공");
        }

        return ResponseEntity.badRequest().body("재료 등록 실패");
    }

    @Operation(summary = "식재료 수정", description = "기본키 id를 기준으로 식재료 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<String> updateIngredient(
            @Parameter(description = "수정할 식재료 기본키 id", example = "1")
            @PathVariable int id,
            @RequestBody Ingredient ingredient
    ) {
        ingredient.setId(id);

        boolean result = ingredientService.updateIngredient(ingredient);

        if (result) {
            return ResponseEntity.ok("재료 수정 성공");
        }

        return ResponseEntity.badRequest().body("재료 수정 실패");
    }

    @Operation(summary = "식재료 삭제", description = "기본키 id를 기준으로 식재료 정보를 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteIngredient(
            @Parameter(description = "삭제할 식재료 기본키 id", example = "1")
            @PathVariable int id
    ) {
        boolean result = ingredientService.deleteIngredient(id);

        if (result) {
            return ResponseEntity.ok("재료 삭제 성공");
        }

        return ResponseEntity.badRequest().body("재료 삭제 실패");
    }
}