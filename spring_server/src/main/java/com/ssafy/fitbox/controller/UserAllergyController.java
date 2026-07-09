package com.ssafy.fitbox.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ssafy.fitbox.service.UserAllergyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User Allergy API", description = "회원 알레르기 정보 CRUD API")
@RestController
@CrossOrigin("*")
@RequestMapping("/users/{userId}/allergies") // RESTful 한 경로 설계
public class UserAllergyController {

    private final UserAllergyService userAllergyService;

    // 생성자 주입
    public UserAllergyController(UserAllergyService userAllergyService) {
        this.userAllergyService = userAllergyService;
    }

    @Operation(summary = "회원 알레르기 목록 조회", description = "특정 회원의 알레르기 목록을 문자열 배열로 반환합니다.")
    @GetMapping
    public ResponseEntity<List<String>> getUserAllergies(
            @Parameter(description = "회원 기본키 id", example = "1")
            @PathVariable int userId
    ) {
        List<String> allergies = userAllergyService.getUserAllergies(userId);
        return ResponseEntity.ok(allergies);
    }

    @Operation(summary = "회원 알레르기 정보 업데이트", description = "기존 알레르기 정보를 모두 삭제하고, 전달받은 새로운 목록으로 덮어씁니다.")
    @PostMapping
    public ResponseEntity<String> updateUserAllergies(
            @Parameter(description = "회원 기본키 id", example = "1")
            @PathVariable int userId,
            
            @Parameter(description = "등록할 알레르기 이름 목록", example = "[\"계란\", \"우유\"]")
            @RequestBody List<String> allergies
    ) {
        boolean result = userAllergyService.updateUserAllergies(userId, allergies);

        if (result) {
            return ResponseEntity.ok("알레르기 업데이트 성공");
        }

        return ResponseEntity.badRequest().body("알레르기 업데이트 실패");
    }
}