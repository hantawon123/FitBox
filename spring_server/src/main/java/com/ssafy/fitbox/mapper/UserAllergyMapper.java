package com.ssafy.fitbox.mapper;

import com.ssafy.fitbox.dto.UserAllergy;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface UserAllergyMapper {
    // 1. 특정 유저의 모든 알레르기 목록 조회
    List<String> selectAllergiesByUserId(int userId);
    
    // 2. 알레르기 등록
    int insertAllergy(UserAllergy userAllergy);
    
    // 3. 특정 유저의 알레르기 전체 삭제 (수정 시 싹 비우고 다시 넣기 위해 필요)
    int deleteAllergiesByUserId(int userId);
}