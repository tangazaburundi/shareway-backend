package com.shareway.application.dto.response;
import lombok.*; import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReportResponse { private String id,targetType,targetId,reason,description,status,actionTaken; private UserResponse reporter; private LocalDateTime createdAt; }
