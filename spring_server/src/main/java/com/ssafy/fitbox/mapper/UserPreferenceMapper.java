package com.ssafy.fitbox.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.ssafy.fitbox.dto.UserPreference;

@Mapper
public interface UserPreferenceMapper {
    UserPreference selectByUserId(int userId);
    int insertOrUpdate(UserPreference preference);
}