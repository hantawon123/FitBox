package com.ssafy.fitbox.dto;

import java.util.List;

public class User {
    private int id;
    private String userId;
    private String password;
    private String name;
    private String phone;
    private String gender;
    private int age;
    private double height;
    private double weight;
    private int activityLevel;
    private List<String> allergies;
    
    private String purpose;

    public User() {}

    public User(int id, String userId, String password, String name, String phone, String gender, int age, double height, double weight, int activityLevel, String purpose) {
        this.id = id;
        this.userId = userId;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.gender = gender;
        this.age = age;
        this.height = height;               
        this.weight = weight;            
        this.activityLevel = activityLevel; 
        this.purpose = purpose;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = height; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    public int getActivityLevel() { return activityLevel; }
    public void setActivityLevel(int activityLevel) { this.activityLevel = activityLevel; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    
    public List<String> getAllergies() { return allergies; }
    public void setAllergies(List<String> allergies) { this.allergies = allergies; }
}