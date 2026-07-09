package com.ssafy.fitbox.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingClass("com.google.firebase.messaging.FirebaseMessaging")
public class UnavailableFcmGateway implements FcmGateway {

    private static final Logger log = LoggerFactory.getLogger(UnavailableFcmGateway.class);

    @Override
    public boolean send(
            Integer userId,
            String token,
            String title,
            String message
    ) {
        log.warn(
            "Firebase Admin SDK가 실행 클래스패스에 없어 사용자 {} 알림을 알림함에만 저장했습니다.",
            userId
        );
        return false;
    }
}
