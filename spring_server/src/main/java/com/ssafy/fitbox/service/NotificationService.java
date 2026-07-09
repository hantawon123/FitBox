package com.ssafy.fitbox.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.ssafy.fitbox.dto.response.NotificationResponse;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final JdbcTemplate jdbcTemplate;
    private final FcmGateway fcmGateway;

    public NotificationService(JdbcTemplate jdbcTemplate, FcmGateway fcmGateway) {
        this.jdbcTemplate = jdbcTemplate;
        this.fcmGateway = fcmGateway;
    }

    public void registerToken(Integer userId, String token) {
        if (userId == null || token == null || token.isBlank()) {
            throw new IllegalArgumentException("userId와 FCM token이 필요합니다.");
        }
        jdbcTemplate.update("""
            INSERT INTO fcm_user_token_table (user_id, token)
            VALUES (?, ?)
            ON DUPLICATE KEY UPDATE
                updated_at = CURRENT_TIMESTAMP
            """, userId, token);
        log.info("FCM 토큰 등록 완료: userId={}", userId);
    }

    public boolean hasRegisteredToken(Integer userId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM fcm_user_token_table WHERE user_id = ?",
            Integer.class,
            userId
        );
        return count != null && count > 0;
    }

    public boolean send(Integer userId, String title, String message) {
        validateMessage(userId, title, message);
        saveNotification(userId, title, message);
        return sendFcmBestEffort(userId, title, message);
    }

    public void notifyStoreAdmins(Long storeId, String title, String message) {
        if (storeId == null) {
            return;
        }
        List<Integer> adminUserIds = jdbcTemplate.queryForList(
            "SELECT user_id FROM store_admin_table WHERE store_id = ?",
            Integer.class,
            storeId
        );
        for (Integer adminUserId : adminUserIds) {
            send(adminUserId, title, message);
        }
    }

    private boolean sendFcmBestEffort(Integer userId, String title, String message) {
        List<String> tokens = jdbcTemplate.queryForList(
            "SELECT token FROM fcm_user_token_table WHERE user_id = ? ORDER BY updated_at DESC",
            String.class,
            userId
        );
        if (tokens.isEmpty()) {
            log.warn("사용자 {}의 등록된 FCM 토큰이 없어 알림함에만 저장했습니다.", userId);
            return false;
        }

        int successCount = 0;
        for (String token : tokens) {
            if (fcmGateway.send(userId, token, title, message)) {
                successCount++;
            }
        }

        if (successCount == 0) {
            log.warn("사용자 {}의 모든 FCM 발송이 실패했지만 알림함 저장은 완료했습니다.", userId);
        }
        return successCount > 0;
    }

    private void saveNotification(Integer userId, String title, String message) {
        jdbcTemplate.update(
            "INSERT INTO notification_table (user_id, title, message) VALUES (?, ?, ?)",
            userId,
            title,
            message
        );
    }

    public List<NotificationResponse> getNotifications(Integer userId) {
        return jdbcTemplate.query("""
            SELECT id, user_id, title, message, is_read, created_at
            FROM notification_table
            WHERE user_id = ?
            ORDER BY created_at DESC, id DESC
            """,
            (resultSet, rowNum) -> new NotificationResponse(
                resultSet.getLong("id"),
                resultSet.getInt("user_id"),
                resultSet.getString("title"),
                resultSet.getString("message"),
                resultSet.getBoolean("is_read"),
                resultSet.getTimestamp("created_at").toLocalDateTime()
            ),
            userId
        );
    }

    public int getUnreadCount(Integer userId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM notification_table WHERE user_id = ? AND is_read = FALSE",
            Integer.class,
            userId
        );
        return count == null ? 0 : count;
    }

    public void markAllAsRead(Integer userId) {
        jdbcTemplate.update(
            "UPDATE notification_table SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE",
            userId
        );
    }

    private void validateMessage(Integer userId, String title, String message) {
        if (userId == null || title == null || title.isBlank() ||
                message == null || message.isBlank()) {
            throw new IllegalArgumentException("userId, title, message가 필요합니다.");
        }
    }
}
