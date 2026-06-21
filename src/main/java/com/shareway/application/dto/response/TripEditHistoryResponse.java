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
public class TripEditHistoryResponse {
    private String id;
    private String fieldChanged;
    private String oldValue;
    private String newValue;
    private int passengersNotified;
    private LocalDateTime createdAt;
}