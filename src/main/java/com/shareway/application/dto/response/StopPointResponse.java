package com.shareway.application.dto.response;
import lombok.*; import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StopPointResponse { private String id,city,address; private int order; private LocalDateTime arrivalTime; }
