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
public class PassengerResponse {
    private String id, firstName, lastName, avatarUrl, phone, bookingId, bookingStatus;
    private LocalDateTime bookedAt;
}
