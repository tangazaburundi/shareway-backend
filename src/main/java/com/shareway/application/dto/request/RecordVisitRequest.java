package com.shareway.application.dto.request;

import lombok.Data;

@Data
public class RecordVisitRequest {
    private String anonymousId;
    private String pageUrl;
    private String referrer;
    private String userAgent;
    private boolean acceptedCookies;
}
