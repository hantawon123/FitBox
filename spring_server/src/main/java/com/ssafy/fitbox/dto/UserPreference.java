package com.ssafy.fitbox.dto;

public class UserPreference {
    private int id;
    private int userId;
    private String preferencePrompt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getPreferencePrompt() { return preferencePrompt; }
    public void setPreferencePrompt(String preferencePrompt) { this.preferencePrompt = preferencePrompt; }
}