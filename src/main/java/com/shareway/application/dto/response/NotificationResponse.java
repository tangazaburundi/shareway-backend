package com.shareway.application.dto.response;
import lombok.*; import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationResponse { private String id,type,title,body,link; private boolean read; private LocalDateTime createdAt; }
