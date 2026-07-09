package com.ssafy.fitbox.service;

import java.util.List;

import com.ssafy.fitbox.dto.User;

public interface UserService {

    List<User> getUsers();

    User getUser(int id);

    User getUserByUserId(String userId);

    boolean createUser(User user);

    boolean updateUser(User user);

    boolean deleteUser(int id);

    boolean isIdDuplicate(String userId);

    boolean isPhoneDuplicate(String phone);

    User login(String userId, String password);

    User kakaoLogin(String accessToken);

    String findUserId(String name, String phone);

    boolean resetPassword(String userId, String name, String phone, String newPassword);
}
