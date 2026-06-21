package com.shareway.application.dto.response;
import lombok.*; import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReviewResponse {
    private String id,tripId,targetUserId,comment,type;
    private ReviewAuthorResponse author;
    private int rating; private boolean flagged,approved;
    private LocalDateTime createdAt;
}
