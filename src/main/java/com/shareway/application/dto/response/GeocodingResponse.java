package com.shareway.application.dto.response;

import java.util.List;

public record GeocodingResponse(List<GeocodingItem> results) {
    public record GeocodingItem(String displayName, double lat, double lng, String city, String country) {}
}
