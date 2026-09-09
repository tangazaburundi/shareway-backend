package com.shareway.infrastructure.websocket;

import com.shareway.domain.repository.TripRepository;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    /** Cooldown minimal entre deux publications GPS pour un même trajet. */
    private static final long GPS_MIN_INTERVAL_MILLIS = 2_000L;

    private final WebSocketNotificationService notificationService;
    private final SimpMessagingTemplate messaging;
    private final TripRepository tripRepository;

    /** Dernier timestamp de publication GPS par trajet (anti-spam). */
    private final Map<String, AtomicLong> lastGpsUpdate = new ConcurrentHashMap<>();

    @MessageMapping("/trip/{tripId}/location")
    public void updateLocation(@DestinationVariable String tripId,
                               @Payload Map<String, Double> payload, Principal principal) {
        if (principal == null) return;

        Double latObj = payload.get("lat");
        Double lngObj = payload.get("lng");
        if (latObj == null || lngObj == null) {
            log.warn("GPS update rejected: missing lat/lng (user {})", principal.getName());
            return;
        }
        double lat = latObj;
        double lng = lngObj;
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            log.warn("GPS update rejected: out-of-range coordinates ({} , {})", lat, lng);
            return;
        }

        // Auto-refresh du cooldown par trajet : au plus 1 envoi / 2 s, même si
        // le client spamme (ex: plusieurs onglets ou scripts).
        long now = System.currentTimeMillis();
        AtomicLong last = lastGpsUpdate.computeIfAbsent(tripId, k -> new AtomicLong(0L));
        if (now - last.get() < GPS_MIN_INTERVAL_MILLIS) {
            log.warn("GPS update rejected: too frequent for trip {} (user {})", tripId, principal.getName());
            return;
        }

        // Seul le conducteur DU trajet peut publier sa position.
        boolean isDriver = tripRepository.findByIdForUpdate(tripId)
                .map(t -> t.getDriver() != null && t.getDriver().getId().equals(principal.getName()))
                .orElse(false);
        if (!isDriver) {
            log.warn("GPS update rejected: user {} is not the driver of trip {}", principal.getName(), tripId);
            return;
        }

        last.set(now);
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
