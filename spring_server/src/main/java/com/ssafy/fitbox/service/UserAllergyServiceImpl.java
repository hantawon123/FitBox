package com.ssafy.fitbox.service;

import com.ssafy.fitbox.dto.UserAllergy;
import com.ssafy.fitbox.mapper.UserAllergyMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserAllergyServiceImpl implements UserAllergyService {

    private final UserAllergyMapper userAllergyMapper;

    // 생성자 주입 (회원님 스타일)
    public UserAllergyServiceImpl(UserAllergyMapper userAllergyMapper) {
        this.userAllergyMapper = userAllergyMapper;
    }

    @Override
    public List<String> getUserAllergies(int userId) {
        return userAllergyMapper.selectAllergiesByUserId(userId);
    }

    @Override
    @Transactional // 삭제와 삽입이 하나의 트랜잭션으로 안전하게 동작하도록 보장
    public boolean updateUserAllergies(int userId, List<String> allergies) {
        // 1. 회원의 기존 알레르기 정보를 모두 삭제 (초기화)
        userAllergyMapper.deleteAllergiesByUserId(userId);

        // 2. 안드로이드에서 보낸 새 알레르기 목록이 있다면 순회하며 하나씩 삽입
        if (allergies != null && !allergies.isEmpty()) {
            for (String allergyName : allergies) {
                UserAllergy userAllergy = new UserAllergy(userId, allergyName);
                userAllergyMapper.insertAllergy(userAllergy);
            }
        }
        return true;
    }
}