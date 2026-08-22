package com.shareway.infrastructure.adapter.geocoding;

import com.shareway.application.port.out.GeocodingPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
public class NominatimGeocodingAdapter implements GeocodingPort {

    private static final String NOMINATIM_BASE = "https://nominatim.openstreetmap.org";
    private static final long RATE_LIMIT_MS = 1100;

    private final RestClient restClient;
    private final ReentrantLock rateLimitLock = new ReentrantLock();
    private long lastRequestTime = 0;

    public NominatimGeocodingAdapter(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl(NOMINATIM_BASE)
                .defaultHeader("User-Agent", "SharewayApp/1.0 (carpooling platform)")
                .build();
    }

    @Override
    public List<GeocodingResult> autocomplete(String query) {
        rateLimit();

        Map<String, Object>[] raw = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("q", query)
                        .queryParam("format", "jsonv2")
                        .queryParam("limit", 5)
                        .queryParam("addressdetails", 1)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>[]>() {});

        if (raw == null) return List.of();

        return java.util.Arrays.stream(raw)
                .map(this::toResult)
                .toList();
    }

    @Override
    public GeocodingResult geocode(String address) {
        List<GeocodingResult> results = autocomplete(address);
        return results.isEmpty() ? null : results.getFirst();
    }

    @Override
    public String reverseGeocode(double lat, double lng) {
        rateLimit();

        Map<String, Object> raw = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/reverse")
                        .queryParam("lat", lat)
                        .queryParam("lon", lng)
                        .queryParam("format", "jsonv2")
                        .queryParam("addressdetails", 1)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        if (raw == null) return null;

        Map<String, Object> address = (Map<String, Object>) raw.get("address");

        if (address != null) {
            String houseNumber = (String) address.getOrDefault("house_number", "");
            String road = (String) address.getOrDefault("road", "");
            String city = (String) address.getOrDefault("city",
                    address.getOrDefault("town",
                            address.getOrDefault("village", "")));
            String postcode = (String) address.getOrDefault("postcode", "");

            StringBuilder sb = new StringBuilder();
            if (!road.isEmpty()) {
                if (!houseNumber.isEmpty()) {
                    sb.append(houseNumber).append(" ");
                }
                sb.append(road);
            }
            if (!city.isEmpty()) {
                if (!sb.isEmpty()) sb.append(", ");
                sb.append(city);
            }
            if (!postcode.isEmpty()) {
                sb.append(" ").append(postcode);
            }

            if (sb.length() > 0) {
                return sb.toString();
            }
        }

        String displayName = (String) raw.get("display_name");
        return displayName;
    }

    @SuppressWarnings("unchecked")
    private GeocodingResult toResult(Map<String, Object> item) {
        double lat = Double.parseDouble((String) item.get("lat"));
        double lng = Double.parseDouble((String) item.get("lon"));

        Map<String, Object> address = (Map<String, Object>) item.get("address");
        String city = address != null ? (String) address.getOrDefault("city",
                address.getOrDefault("town", address.getOrDefault("village", ""))) : "";
        String country = address != null ? (String) address.getOrDefault("country", "") : "";

        String shortName = buildShortAddress(address);

        return new GeocodingResult(shortName, lat, lng, city, country);
    }

    @SuppressWarnings("unchecked")
    private String buildShortAddress(Map<String, Object> address) {
        if (address == null) return "";

        String houseNumber = (String) address.getOrDefault("house_number", "");
        String road = (String) address.getOrDefault("road", "");
        String city = (String) address.getOrDefault("city",
                address.getOrDefault("town",
                        address.getOrDefault("village", "")));
        String postcode = (String) address.getOrDefault("postcode", "");

        StringBuilder sb = new StringBuilder();
        if (!road.isEmpty()) {
            if (!houseNumber.isEmpty()) {
                sb.append(houseNumber).append(" ");
            }
            sb.append(road);
        }
        if (!city.isEmpty()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(city);
        }
        if (!postcode.isEmpty()) {
            sb.append(" ").append(postcode);
        }
        return sb.toString();
    }

    private void rateLimit() {
        rateLimitLock.lock();
        try {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRequestTime;
            if (elapsed < RATE_LIMIT_MS) {
                try {
                    Thread.sleep(RATE_LIMIT_MS - elapsed);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            lastRequestTime = System.currentTimeMillis();
        } finally {
            rateLimitLock.unlock();
        }
    }
}
