package com.shareway.application.dto.response;
import lombok.*; import java.math.BigDecimal;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TripUserResponse { private String id,firstName,lastName,avatarUrl,phone; private BigDecimal rating; private boolean verified; }
