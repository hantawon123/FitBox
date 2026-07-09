package com.ssafy.fitbox.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ssafy.fitbox.dto.UserPreference;
import com.ssafy.fitbox.mapper.UserPreferenceMapper;

@RestController
@CrossOrigin("*")
@RequestMapping("/preferences")
public class PreferenceController {
    private final UserPreferenceMapper preferenceMapper;

    public PreferenceController(UserPreferenceMapper preferenceMapper) {
        this.preferenceMapper = preferenceMapper;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserPreference> getPreference(@PathVariable int userId) {
        UserPreference pref = preferenceMapper.selectByUserId(userId);
        return ResponseEntity.ok(pref); // 없으면 null 반환됨
    }

    @PostMapping
    public ResponseEntity<String> savePreference(@RequestBody UserPreference preference) {
        preferenceMapper.insertOrUpdate(preference);
        return ResponseEntity.ok("취향 정보 저장 완료");
    }
}