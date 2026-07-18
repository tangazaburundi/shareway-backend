package com.shareway.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitorRowResponse {
    private String id;
    private String userName;
    private String userEmail;
    private String anonymousId;
    private String country;
    private String city;
    private String pageUrl;
    private boolean acceptedCookies;
    private LocalDateTime visitedAt;
}
