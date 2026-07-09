package com.ssafy.fitbox.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.fitbox.dto.request.FcmTokenRequest;
import com.ssafy.fitbox.dto.request.PushNotificationRequest;
import com.ssafy.fitbox.dto.response.NotificationResponse;
import com.ssafy.fitbox.service.NotificationService;

@RestController
@CrossOrigin("*")
@RequestMapping("/notificationapi")
public class NotificationRestController {

    private final NotificationService notificationService;

    public NotificationRestController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/tokens")
    public ResponseEntity<String> registerToken(@RequestBody FcmTokenRequest request) {
        notificationService.registerToken(request.getUserId(), request.getToken());
        return ResponseEntity.ok("FCM 토큰 등록 성공");
    }

    @GetMapping("/tokens/users/{userId}")
    public ResponseEntity<Boolean> hasRegisteredToken(@PathVariable Integer userId) {
        return ResponseEntity.ok(notificationService.hasRegisteredToken(userId));
    }

    @PostMapping("/send")
    public ResponseEntity<String> send(@RequestBody PushNotificationRequest request) {
        try {
            boolean sent = notificationService.send(
                request.getUserId(),
                request.getTitle(),
                request.getMessage()
            );
            if (!sent) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("알림함에는 저장했지만 FCM 푸시 발송에 실패했습니다. Firebase 서비스 계정 설정을 확인해주세요.");
            }
            return ResponseEntity.ok("FCM 알림 발송 성공");
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(error.getMessage());
        } catch (IllegalStateException error) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(error.getMessage());
        }
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @PathVariable Integer userId
    ) {
        return ResponseEntity.ok(notificationService.getNotifications(userId));
    }

    @GetMapping("/users/{userId}/unread-count")
    public ResponseEntity<Integer> getUnreadCount(@PathVariable Integer userId) {
        return ResponseEntity.ok(notificationService.getUnreadCount(userId));
    }

    @PutMapping("/users/{userId}/read-all")
    public ResponseEntity<Void> markAllAsRead(@PathVariable Integer userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }
}
