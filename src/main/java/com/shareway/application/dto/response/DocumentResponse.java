package com.shareway.application.dto.response;
import lombok.*; import java.time.LocalDate; import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DocumentResponse { private String id,type,fileUrl,fileName,status,rejectionReason; private LocalDate expiresAt; private LocalDateTime createdAt; }
