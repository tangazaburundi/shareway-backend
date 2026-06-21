package com.shareway.application.usecase;

import com.shareway.application.dto.response.NotificationResponse;
import com.shareway.application.dto.response.PageResponse;
import com.shareway.infrastructure.adapter.audit.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationUseCase {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.from(
                notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                        .map(n -> NotificationResponse.builder()
                                .id(n.getId()).type(n.getType().name()).title(n.getTitle())
                                .body(n.getBody()).link(n.getLink()).read(n.isRead())
                                .createdAt(n.getCreatedAt()).build()));
    }

    @Transactional(readOnly = true)
    public long countUnread(String userId) {
        return notificationRepository.countUnread(userId);
    }

    public void markAllAsRead(String userId) {
        notificationRepository.markAllAsRead(userId);
    }

    public void markAsRead(String notificationId, String userId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getUser().getId().equals(userId)) {
                n.markAsRead();
                notificationRepository.save(n);
            }
        });
    }
}
