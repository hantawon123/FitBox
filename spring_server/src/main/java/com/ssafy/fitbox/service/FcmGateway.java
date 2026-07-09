package com.ssafy.fitbox.service;

public interface FcmGateway {
    boolean send(Integer userId, String token, String title, String message);
}
