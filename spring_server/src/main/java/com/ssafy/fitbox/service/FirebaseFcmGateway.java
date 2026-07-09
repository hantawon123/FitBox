package com.ssafy.fitbox.service;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.Message;

@Component
@ConditionalOnClass(name = "com.google.firebase.messaging.FirebaseMessaging")
public class FirebaseFcmGateway implements FcmGateway {

    private static final Logger log = LoggerFactory.getLogger(FirebaseFcmGateway.class);

    @Value("${firebase.service-account.path:}")
    private String serviceAccountPath;

    @Override
    public boolean send(
            Integer userId,
            String token,
            String title,
            String message
    ) {
        try {
            Message firebaseMessage = Message.builder()
                .setToken(token)
                .setAndroidConfig(AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .build())
                .putData("title", title)
                .putData("message", message)
                .putData("userId", String.valueOf(userId))
                .build();
            getFirebaseMessaging().send(firebaseMessage);
            return true;
        } catch (Exception error) {
            log.error("사용자 {}에게 FCM 발송 실패", userId, error);
            return false;
        }
    }

    private FirebaseMessaging getFirebaseMessaging() throws IOException {
        FirebaseApp app;
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(loadCredentials())
                .setProjectId("fitbox-3d68f")
                .build();
            app = FirebaseApp.initializeApp(options);
        } else {
            app = FirebaseApp.getInstance();
        }
        return FirebaseMessaging.getInstance(app);
    }

    private GoogleCredentials loadCredentials() throws IOException {
        if (serviceAccountPath != null && !serviceAccountPath.isBlank()) {
            try (InputStream inputStream = new FileInputStream(serviceAccountPath)) {
                log.info("Firebase 서비스 계정 키를 로드했습니다: {}", serviceAccountPath);
                return GoogleCredentials.fromStream(inputStream);
            }
        }
        return GoogleCredentials.getApplicationDefault();
    }
}
