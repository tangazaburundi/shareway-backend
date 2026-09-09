package com.shareway.infrastructure.config;

import com.shareway.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);
            if (token != null) {
                try {
                    if (jwtService.isValidAccessToken(token)) {
                        String userId = jwtService.extractUserId(token);
                        String role = jwtService.extractRole(token);
                        String systemRole = jwtService.extractSystemeRole(token);
                        var auth = new UsernamePasswordAuthenticationToken(
                                userId, null,
                                List.of(
                                        new SimpleGrantedAuthority("ROLE_" + role),
                                        new SimpleGrantedAuthority("ROLE_" + systemRole)
                                ));
                        accessor.setUser(auth);
                        log.info("WebSocket CONNECT authenticated user: {} (role={})", userId, role);
                    } else {
                        log.warn("WebSocket CONNECT: invalid JWT token");
                    }
                } catch (Exception e) {
                    log.warn("WebSocket CONNECT auth failed: {}", e.getMessage());
                }
            } else {
                log.warn("WebSocket CONNECT: no token found in headers or URL");
            }
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            java.security.Principal principalAuth = accessor.getUser();
            if (principalAuth == null || destination == null) {
                log.warn("WebSocket SUBSCRIBE rejected: no auth or no destination");
                return message;
            }

            String userId = principalAuth.getName();
            boolean isAdmin = principalAuth instanceof org.springframework.security.core.Authentication auth
                    && auth.getAuthorities() != null
                    && auth.getAuthorities().stream().anyMatch(
                    a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_SUPER_ADMIN".equals(a.getAuthority()));

            if (destination.startsWith("/user/") && destination.contains("/queue/")) {
                String expectedPrefix = "/user/" + userId + "/queue/";
                if (!destination.startsWith(expectedPrefix)) {
                    log.warn("WebSocket SUBSCRIBE rejected: {} not allowed for user {}", destination, userId);
                    return message;
                }
            } else if (destination.startsWith("/topic/trip/")) {
                // Les topics de localisation par trajet sont ouverts aux participants.
                // Vérification basique : le topic a le format /topic/trip/{tripId}/location.
                // TODO: vérifier que l'utilisateur est bien un participant du trajet (booké + driver).
                // Pour l'instant on bloque sauf ADMIN/SUPER_ADMIN qui auditent.
                if (!isAdmin) {
                    log.warn("WebSocket SUBSCRIBE rejected on trip topic for non-admin: {} by {}", destination, userId);
                    return message;
                }
            } else if (destination.startsWith("/topic/admin/sos")) {
                if (!isAdmin) {
                    log.warn("WebSocket SUBSCRIBE rejected on admin SOS: {} by {}", destination, userId);
                    return message;
                }
            } else if (destination.startsWith("/topic/")) {
                log.warn("WebSocket SUBSCRIBE rejected on public topic: {} by {}", destination, userId);
                return message;
            }
        }

        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        // 1. STOMP header "Authorization" (from CONNECT frame)
        String authHeader = accessor.getHeader("Authorization") instanceof String h ? h : null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // 2. Native header "Authorization" (from HTTP handshake)
        String nativeAuth = accessor.getFirstNativeHeader("Authorization");
        if (nativeAuth != null && nativeAuth.startsWith("Bearer ")) {
            return nativeAuth.substring(7);
        }

        // 3. Query parameter ?token=xxx (from WebSocket URL)
        Object connectHeader = accessor.getHeader("token");
        if (connectHeader instanceof String t && !t.isEmpty()) {
            return t;
        }

        return null;
    }
}
