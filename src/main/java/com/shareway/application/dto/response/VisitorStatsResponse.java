package com.shareway.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitorStatsResponse {
    private long totalVisits;
    private long uniqueUsers;
    private long uniqueAnonymous;
    private long totalAnonymous;
    private long cookiesAccepted;
    private long cookiesRejected;
    private Map<String, Long> visitsByCountry;
    private Map<String, Long> visitsByCity;
    private Map<String, Long> visitsByDay;
}
