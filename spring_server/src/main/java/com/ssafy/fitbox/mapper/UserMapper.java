package com.ssafy.fitbox.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.ssafy.fitbox.dto.User;

@Mapper
public interface UserMapper {
    List<User> selectAll();
    User selectById(int id);
    User selectByUserId(String userId);
    int insert(User user);
    int update(User user);
    int delete(int id);
    User selectByPhone(String phone);
    User selectByNameAndPhone(@Param("name") String name, @Param("phone") String phone);
    User selectByUserIdNameAndPhone(
            @Param("userId") String userId,
            @Param("name") String name,
            @Param("phone") String phone
    );
    int updatePasswordById(@Param("id") int id, @Param("password") String password);
}
