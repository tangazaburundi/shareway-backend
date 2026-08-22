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
                    if (jwtService.isValid(token)) {
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
        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        // 1. STOMP header "Authorization" (from CONNECT frame)
        String authHeader = accessor.getHeader("Authorization") instanceof String h ? h : null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            log.debug("Token from STOMP Authorization header");
            return authHeader.substring(7);
        }

        // 2. Native header "Authorization" (from HTTP handshake)
        String nativeAuth = accessor.getFirstNativeHeader("Authorization");
        if (nativeAuth != null && nativeAuth.startsWith("Bearer ")) {
            log.debug("Token from native Authorization header");
            return nativeAuth.substring(7);
        }

        // 3. Query parameter ?token=xxx (from WebSocket URL)
        // The token may be embedded in the session attributes during handshake
        Object connectHeader = accessor.getHeader("token");
        if (connectHeader instanceof String t && !t.isEmpty()) {
            log.debug("Token from STOMP 'token' header");
            return t;
        }

        log.debug("No token found in STOMP headers for command: {}", accessor.getCommand());
        return null;
    }
}
