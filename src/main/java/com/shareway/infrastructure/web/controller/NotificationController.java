package com.shareway.infrastructure.web.controller;

import com.shareway.application.dto.response.ApiResponse;
import com.shareway.application.dto.response.NotificationResponse;
import com.shareway.application.dto.response.PageResponse;
import com.shareway.application.port.out.PushNotificationPort;
import com.shareway.application.usecase.NotificationUseCase;
import com.shareway.infrastructure.security.SecurityUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Centre de notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationUseCase notificationUseCase;
    private final PushNotificationPort pushNotificationPort;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                notificationUseCase.getNotifications(SecurityUtils.currentUserId(), page, size)));
    }

    @GetMapping("/unread/count")
    public ResponseEntity<ApiResponse<Long>> unreadCount() {
        return ResponseEntity.ok(ApiResponse.ok(
                notificationUseCase.countUnread(SecurityUtils.currentUserId())));

    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead() {
        notificationUseCase.markAllAsRead(SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.noContent("All notifications marked as read"));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable String id) {
        notificationUseCase.markAsRead(id, SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.noContent("Notification marked as read"));
    }

    // ── Push Notification Device Token ──────────────────────────────

    @PostMapping("/device-token")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> registerDeviceToken(@RequestBody Map<String, String> body) {
        String token = body.getOrDefault("token", "");
        String platform = body.getOrDefault("platform", "WEB");
        pushNotificationPort.registerDeviceToken(SecurityUtils.currentUserId(), token, platform);
        return ResponseEntity.ok(ApiResponse.noContent("Device token registered"));
    }
}
