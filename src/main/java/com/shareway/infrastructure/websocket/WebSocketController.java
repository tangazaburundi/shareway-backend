package com.shareway.infrastructure.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final WebSocketNotificationService notificationService;
    private final SimpMessagingTemplate messaging;

    @MessageMapping("/trip/{tripId}/location")
    public void updateLocation(@DestinationVariable String tripId,
                               @Payload Map<String, Double> payload, Principal principal) {
        if (principal == null) return;
        double lat = payload.getOrDefault("lat", 0.0);
        double lng = payload.getOrDefault("lng", 0.0);
        notificationService.broadcastTripLocation(tripId, lat, lng);
        log.debug("GPS update for trip {}: {},{}", tripId, lat, lng);
    }

    @MessageMapping("/chat.typing")
    public void typingIndicator(@Payload Map<String, Object> payload, Principal principal) {
        if (principal == null) return;
        String conversationId = (String) payload.get("conversationId");
        String userId = (String) payload.get("userId");
        boolean isTyping = Boolean.TRUE.equals(payload.get("isTyping"));
        messaging.convertAndSend(
                "/topic/conversation." + conversationId + "/typing",
                Map.of("userId", userId, "isTyping", isTyping, "timestamp", LocalDateTime.now().toString())
        );
    }

    @MessageMapping("/chat.read")
    public void markConversationRead(@Payload Map<String, Object> payload, Principal principal) {
        if (principal == null) return;
        String conversationId = (String) payload.get("conversationId");
        String userId = (String) payload.get("userId");
        messaging.convertAndSend(
                "/topic/conversation." + conversationId + "/read",
                Map.of("userId", userId, "timestamp", LocalDateTime.now().toString())
        );
    }

    @SubscribeMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok", "time", java.time.LocalDateTime.now().toString());
    }
}
