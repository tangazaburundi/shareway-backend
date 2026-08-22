package com.shareway.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsConfigResponse {

    private String id;
    private String provider;
    private boolean enabled;
    private String apiKey;
    private String apiSecret;
    private String senderNumber;
    private String senderName;
}
