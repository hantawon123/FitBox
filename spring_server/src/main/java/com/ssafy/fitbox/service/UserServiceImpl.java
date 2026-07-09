package com.ssafy.fitbox.service;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.ssafy.fitbox.dto.User;
import com.ssafy.fitbox.dto.UserAllergy;
import com.ssafy.fitbox.mapper.UserAllergyMapper;
import com.ssafy.fitbox.mapper.UserMapper;

@Service
public class UserServiceImpl implements UserService {

    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String KAKAO_USER_ID_PREFIX = "kakao_";
    private static final String KAKAO_LOGIN_PASSWORD = "KAKAO_LOGIN";

    private final UserMapper userMapper;
    private final UserAllergyMapper userAllergyMapper;

    public UserServiceImpl(UserMapper userMapper, UserAllergyMapper userAllergyMapper) {
        this.userMapper = userMapper;
        this.userAllergyMapper = userAllergyMapper;
    }

    @Override
    public List<User> getUsers() {
        List<User> users = userMapper.selectAll();
        users.forEach(this::attachAllergies);
        return users;
    }

    @Override
    public User getUser(int id) {
        return attachAllergies(userMapper.selectById(id));
    }

    @Override
    public User getUserByUserId(String userId) {
        return attachAllergies(userMapper.selectByUserId(userId));
    }

    @Override
    @Transactional
    public boolean createUser(User user) {
        boolean isCreated = userMapper.insert(user) == 1;

        if (isCreated) {
            if (user.getAllergies() != null && !user.getAllergies().isEmpty()) {
                for (String allergyName : user.getAllergies()) {
                    UserAllergy userAllergy = new UserAllergy(user.getId(), allergyName);
                    userAllergyMapper.insertAllergy(userAllergy);
                }
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean updateUser(User user) {
        return userMapper.update(user) == 1;
    }

    @Override
    public boolean deleteUser(int id) {
        return userMapper.delete(id) == 1;
    }

    @Override
    public boolean isIdDuplicate(String userId) {
        return userMapper.selectByUserId(userId) != null;
    }

    @Override
    public boolean isPhoneDuplicate(String phone) {
        return userMapper.selectByPhone(phone) != null;
    }

    @Override
    public User login(String userId, String password) {
        User user = userMapper.selectByUserId(userId);

        if (user != null && user.getPassword().equals(password)) {
            return attachAllergies(user);
        }

        return null;
    }

    @Override
    public String findUserId(String name, String phone) {
        if (isBlank(name) || isBlank(phone)) {
            return null;
        }

        User user = userMapper.selectByNameAndPhone(name.trim(), phone.trim());
        if (user == null) {
            return null;
        }

        return user.getUserId();
    }

    @Override
    public boolean resetPassword(String userId, String name, String phone, String newPassword) {
        if (isBlank(userId) || isBlank(name) || isBlank(phone) || isBlank(newPassword)) {
            return false;
        }

        User user = userMapper.selectByUserIdNameAndPhone(
                userId.trim(),
                name.trim(),
                phone.trim()
        );
        if (user == null) {
            return false;
        }

        return userMapper.updatePasswordById(user.getId(), newPassword.trim()) == 1;
    }

    @Override
    @Transactional
    public User kakaoLogin(String accessToken) {
        Map<String, Object> kakaoUserInfo = requestKakaoUserInfo(accessToken);

        if (kakaoUserInfo == null || kakaoUserInfo.get("id") == null) {
            return null;
        }

        String kakaoId = String.valueOf(kakaoUserInfo.get("id"));
        String fitBoxUserId = KAKAO_USER_ID_PREFIX + kakaoId;

        String nickname = extractKakaoNickname(kakaoUserInfo);

        User existingUser = userMapper.selectByUserId(fitBoxUserId);

        if (existingUser != null) {
            if (nickname != null && !nickname.isBlank()
                && !nickname.equals(existingUser.getName())) {
                existingUser.setName(nickname);
                userMapper.update(existingUser);
                return attachAllergies(userMapper.selectByUserId(fitBoxUserId));
            }

            return attachAllergies(existingUser);
        }
        
        User newUser = new User();
        newUser.setUserId(fitBoxUserId);
        newUser.setPassword(KAKAO_LOGIN_PASSWORD);
        newUser.setName(nickname);
        newUser.setPhone(createKakaoPhoneValue(kakaoId));
        newUser.setGender("");
        newUser.setAge(0);
        newUser.setHeight(0.0);
        newUser.setWeight(0.0);
        newUser.setActivityLevel(0);
        newUser.setPurpose("USER");

        return newUser;
    }

    private User attachAllergies(User user) {
        if (user == null || user.getId() <= 0) {
            return user;
        }

        user.setAllergies(userAllergyMapper.selectAllergiesByUserId(user.getId()));
        return user;
    }

    private Map<String, Object> requestKakaoUserInfo(String accessToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    KAKAO_USER_INFO_URL,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                return null;
            }

            return response.getBody();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String extractKakaoNickname(Map<String, Object> kakaoUserInfo) {
        try {
            Object kakaoAccountObject = kakaoUserInfo.get("kakao_account");

            if (!(kakaoAccountObject instanceof Map)) {
                return "카카오사용자";
            }

            Map<String, Object> kakaoAccount = (Map<String, Object>) kakaoAccountObject;
            Object profileObject = kakaoAccount.get("profile");

            if (!(profileObject instanceof Map)) {
                return "카카오사용자";
            }

            Map<String, Object> profile = (Map<String, Object>) profileObject;
            Object nicknameObject = profile.get("nickname");

            if (nicknameObject == null || String.valueOf(nicknameObject).isBlank()) {
                return "카카오사용자";
            }

            return String.valueOf(nicknameObject);

        } catch (Exception e) {
            return "카카오사용자";
        }
    }

    private String createKakaoPhoneValue(String kakaoId) {
        return "KAKAO_" + kakaoId;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
