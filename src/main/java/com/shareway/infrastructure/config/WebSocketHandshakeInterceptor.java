package com.shareway.infrastructure.config;

import com.shareway.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;

    public WebSocketHandshakeInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractTokenFromQuery(request);
        if (token == null) {
            log.warn("WebSocket handshake: no token in query string");
            return true;
        }

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
                attributes.put("SPRING_SECURITY_CONTEXT", new org.springframework.security.core.context.SecurityContextImpl(auth));
                log.info("WebSocket handshake authenticated user: {}", userId);
            } else {
                log.warn("WebSocket handshake: invalid token");
            }
        } catch (Exception e) {
            log.warn("WebSocket handshake auth failed: {}", e.getMessage());
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }

    private String extractTokenFromQuery(ServerHttpRequest request) {
        URI uri = request.getURI();
        String query = uri.getQuery();
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && "token".equals(kv[0]) && !kv[1].isEmpty()) {
                return kv[1];
            }
        }
        return null;
    }
}
