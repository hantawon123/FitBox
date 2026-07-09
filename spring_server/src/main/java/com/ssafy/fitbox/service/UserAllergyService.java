package com.ssafy.fitbox.service;

import java.util.List;

public interface UserAllergyService {
    // 회원의 알레르기 목록 조회
    List<String> getUserAllergies(int userId);
    
    // 회원의 알레르기 정보 업데이트 (덮어쓰기)
    boolean updateUserAllergies(int userId, List<String> allergies);
}