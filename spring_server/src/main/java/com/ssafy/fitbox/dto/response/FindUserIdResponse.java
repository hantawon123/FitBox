package com.ssafy.fitbox.dto.response;

public class FindUserIdResponse {
    private String userId;

    public FindUserIdResponse() {
    }

    public FindUserIdResponse(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
