package com.shareway.infrastructure.notification;

import com.shareway.application.port.out.PushNotificationPort;
import com.shareway.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Push notification implementation using WebSocket as the primary delivery mechanism.
 * In production, this would integrate with Firebase Cloud Messaging (FCM) or Web Push API.
 * For now, we use WebSocket broadcasting + in-app notification bell.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebPushNotificationService implements PushNotificationPort {

    private final UserRepository userRepository;
    private final com.shareway.infrastructure.websocket.WebSocketNotificationService wsNotificationService;

    @Override
    @Async
    public void sendPushNotification(String userId, String title, String body, Map<String, Object> data) {
        try {
            // Send via WebSocket (real-time in-app)
            wsNotificationService.sendNotification(userId, title, body, data);

            // In production: send via FCM/Web Push here
            // FirebaseMessaging.getInstance().send(...)

            log.info("Push notification sent to user {}: {}", userId, title);
        } catch (Exception e) {
            log.warn("Failed to send push notification to user {}: {}", userId, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendPushNotificationToAdmins(String title, String body, Map<String, Object> data) {
        try {
            userRepository.findAll().stream()
                    .filter(u -> u.getSystemRole() != null
                            && (u.getSystemRole().name().equals("SUPER_ADMIN")
                            || u.getSystemRole().name().equals("ADMIN")))
                    .forEach(admin -> {
                        wsNotificationService.sendNotification(admin.getId(), title, body, data);
                    });

            log.info("Push notification sent to all admins: {}", title);
        } catch (Exception e) {
            log.warn("Failed to send push notification to admins: {}", e.getMessage());
        }
    }

    @Override
    @Async
    public void broadcastPushNotification(String title, String body, Map<String, Object> data) {
        try {
            // Use WebSocket global topic for broadcast
            wsNotificationService.sendGlobalNotification(title, body, data);
            log.info("Global push notification broadcast: {}", title);
        } catch (Exception e) {
            log.warn("Failed to broadcast push notification: {}", e.getMessage());
        }
    }

    @Override
    public void registerDeviceToken(String userId, String token, String platform) {
        // Store device token for FCM/Web Push delivery
        // In production: save to a device_tokens table
        log.info("Device token registered for user {} (platform: {}): {}...",
                userId, platform, token.length() > 20 ? token.substring(0, 20) : token);
    }
}
