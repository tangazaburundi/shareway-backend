package com.shareway.application.port.out;

import java.util.List;

public interface GeocodingPort {
    List<GeocodingResult> autocomplete(String query);
    GeocodingResult geocode(String address);
    String reverseGeocode(double lat, double lng);

    record GeocodingResult(String displayName, double lat, double lng, String city, String country) {}
}
