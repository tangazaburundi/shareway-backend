package com.shareway.application.dto.response;
import lombok.*; import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MessageResponse { private String id,senderId,receiverId,content; private boolean read; private LocalDateTime createdAt; }
