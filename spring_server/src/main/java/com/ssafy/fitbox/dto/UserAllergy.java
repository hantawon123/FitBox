package com.ssafy.fitbox.dto;

public class UserAllergy {
    private Long id;
    private int userId;
    private String allergyName;
    private String createdAt;

    // 기본 생성자
    public UserAllergy() {}

    public UserAllergy(int userId, String allergyName) {
        this.userId = userId;
        this.allergyName = allergyName;
    }

    // Getter & Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getAllergyName() { return allergyName; }
    public void setAllergyName(String allergyName) { this.allergyName = allergyName; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}