package com.shareway.infrastructure.websocket;

import com.shareway.application.port.out.NotificationPort;
import com.shareway.infrastructure.adapter.audit.domain.model.Notification;
import com.shareway.infrastructure.adapter.audit.domain.model.User;
import com.shareway.infrastructure.adapter.audit.domain.repository.NotificationRepository;
import com.shareway.infrastructure.adapter.audit.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService implements NotificationPort {

    private final SimpMessagingTemplate messaging;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    public void notify(String userId, String type, String title, String body) {
        notifyWithLink(userId, type, title, body, null);
    }

    @Override
    public void notifyWithLink(String userId, String type, String title, String body, String link) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        Notification notification = Notification.builder()
                .user(user)
                .type(Notification.NotificationType.valueOf(type))
                .title(title).body(body).link(link)
                .build();
        notificationRepository.save(notification);

        // Push WebSocket
        Map<String, Object> payload = Map.of(
                "id", notification.getId(),
                "type", type, "title", title, "body", body,
                "link", link != null ? link : "",
                "createdAt", LocalDateTime.now().toString()
        );
        messaging.convertAndSendToUser(userId, "/queue/notifications", payload);
        log.debug("WS notification sent to user {}: {}", userId, title);
    }

    @Override
    public void broadcast(String type, String title, String body) {
        Map<String, Object> payload = Map.of("type", type, "title", title, "body", body);
        messaging.convertAndSend("/topic/global", payload);
    }

    /**
     * Push de position GPS en temps réel
     */
    public void broadcastTripLocation(String tripId, double lat, double lng) {
        Map<String, Object> payload = Map.of("tripId", tripId, "lat", lat, "lng", lng);
        messaging.convertAndSend("/topic/trip/" + tripId + "/location", payload);
    }
}
