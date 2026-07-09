package com.ssafy.fitbox.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ssafy.fitbox.dto.request.*;
import com.ssafy.fitbox.dto.User;
import com.ssafy.fitbox.dto.response.FindUserIdResponse;
import com.ssafy.fitbox.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User API", description = "회원 정보 CRUD API")
@RestController
@CrossOrigin("*")
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "회원 전체 조회", description = "user_table에 저장된 모든 회원 정보를 조회합니다.")
    @GetMapping
    public ResponseEntity<List<User>> getUsers() {
        return ResponseEntity.ok(userService.getUsers());
    }

    @Operation(summary = "회원 단건 조회", description = "기본키 id를 기준으로 회원 한 명을 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(
            @Parameter(description = "회원 기본키 id", example = "1")
            @PathVariable int id
    ) {
        User user = userService.getUser(id);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(user);
    }

    @Operation(summary = "회원 아이디로 조회", description = "로그인 아이디 user_id를 기준으로 회원 정보를 조회합니다.")
    @GetMapping("/user-id/{userId}")
    public ResponseEntity<User> getUserByUserId(
            @Parameter(description = "사용자 로그인 아이디", example = "user01")
            @PathVariable String userId
    ) {
        User user = userService.getUserByUserId(userId);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(user);
    }

    @Operation(summary = "회원 등록", description = "새로운 회원 정보를 user_table에 등록합니다.")
    @PostMapping
    public ResponseEntity<String> createUser(@RequestBody User user) {
        boolean result = userService.createUser(user);

        if (result) {
            return ResponseEntity.ok("회원 등록 성공");
        }

        return ResponseEntity.badRequest().body("회원 등록 실패");
    }

    @Operation(summary = "회원 수정", description = "기본키 id를 기준으로 회원 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<String> updateUser(
            @Parameter(description = "수정할 회원 기본키 id", example = "1")
            @PathVariable int id,
            @RequestBody User user
    ) {
        user.setId(id);

        boolean result = userService.updateUser(user);

        if (result) {
            return ResponseEntity.ok("회원 수정 성공");
        }

        return ResponseEntity.badRequest().body("회원 수정 실패");
    }

    @Operation(summary = "회원 삭제", description = "기본키 id를 기준으로 회원 정보를 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(
            @Parameter(description = "삭제할 회원 기본키 id", example = "1")
            @PathVariable int id
    ) {
        boolean result = userService.deleteUser(id);

        if (result) {
            return ResponseEntity.ok("회원 삭제 성공");
        }

        return ResponseEntity.badRequest().body("회원 삭제 실패");
    }

    @Operation(summary = "아이디 중복 검사")
    @GetMapping("/check/id/{userId}")
    public ResponseEntity<Boolean> checkId(@PathVariable String userId) {
        return ResponseEntity.ok(userService.isIdDuplicate(userId));
    }

    @Operation(summary = "전화번호 중복 검사")
    @GetMapping("/check/phone/{phone}")
    public ResponseEntity<Boolean> checkPhone(@PathVariable String phone) {
        return ResponseEntity.ok(userService.isPhoneDuplicate(phone));
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody User user) {
        User loggedInUser = userService.login(user.getUserId(), user.getPassword());

        if (loggedInUser != null) {
            return ResponseEntity.ok(loggedInUser);
        }

        return ResponseEntity.status(401).build();
    }

    @Operation(summary = "카카오 로그인", description = "카카오 accessToken으로 사용자 정보를 조회하고 FitBox 회원으로 로그인합니다.")
    @PostMapping("/find-id")
    public ResponseEntity<FindUserIdResponse> findUserId(@RequestBody FindUserIdRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().build();
        }

        String userId = userService.findUserId(request.getName(), request.getPhone());
        if (userId == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(new FindUserIdResponse(userId));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().body("비밀번호 변경에 필요한 정보가 없습니다.");
        }

        boolean result = userService.resetPassword(
                request.getUserId(),
                request.getName(),
                request.getPhone(),
                request.getNewPassword()
        );

        if (result) {
            return ResponseEntity.ok("비밀번호가 변경되었습니다.");
        }

        return ResponseEntity.status(404).body("입력한 정보와 일치하는 회원을 찾을 수 없습니다.");
    }

    @PostMapping("/login/kakao")
    public ResponseEntity<User> kakaoLogin(@RequestBody KakaoLoginRequest request) {
        if (request == null || request.getAccessToken() == null || request.getAccessToken().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        User loggedInUser = userService.kakaoLogin(request.getAccessToken());

        if (loggedInUser != null) {
            return ResponseEntity.ok(loggedInUser);
        }

        return ResponseEntity.status(401).build();
    }
}
