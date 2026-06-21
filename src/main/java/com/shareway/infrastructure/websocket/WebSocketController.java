package com.shareway.infrastructure.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;


@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final WebSocketNotificationService notificationService;

    /**
     * Mise à jour GPS du conducteur pendant un trajet
     */
    @MessageMapping("/trip/{tripId}/location")
    public void updateLocation(@DestinationVariable String tripId,
                               @Payload Map<String, Double> payload, Principal principal) {
        if (principal == null) return;
        double lat = payload.getOrDefault("lat", 0.0);
        double lng = payload.getOrDefault("lng", 0.0);
        notificationService.broadcastTripLocation(tripId, lat, lng);
        log.debug("GPS update for trip {}: {},{}", tripId, lat, lng);
    }

    @SubscribeMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok", "time", java.time.LocalDateTime.now().toString());
    }
}
