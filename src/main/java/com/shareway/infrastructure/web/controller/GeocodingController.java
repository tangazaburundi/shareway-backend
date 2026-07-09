package com.shareway.infrastructure.web.controller;

import com.shareway.application.dto.response.ApiResponse;
import com.shareway.application.dto.response.GeocodingResponse;
import com.shareway.application.port.out.GeocodingPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/geocoding")
@RequiredArgsConstructor
@Tag(name = "Geocoding", description = "Autocomplete et géocodage via OpenStreetMap Nominatim")
public class GeocodingController {

    private final GeocodingPort geocodingPort;

    @GetMapping("/autocomplete")
    @Operation(summary = "Autocomplete d'adresses", description = "Retourne une liste de suggestions d'adresses via Nominatim")
    public ResponseEntity<ApiResponse<GeocodingResponse>> autocomplete(@RequestParam String q) {
        var results = geocodingPort.autocomplete(q);
        var items = results.stream()
                .map(r -> new GeocodingResponse.GeocodingItem(r.displayName(), r.lat(), r.lng(), r.city(), r.country()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(new GeocodingResponse(items)));
    }

    @GetMapping("/reverse")
    @Operation(summary = "Géocodage inverse", description = "Retourne l'adresse correspondant à des coordonnées")
    public ResponseEntity<ApiResponse<String>> reverse(@RequestParam double lat, @RequestParam double lng) {
        String address = geocodingPort.reverseGeocode(lat, lng);
        return ResponseEntity.ok(ApiResponse.ok(address));
    }
}
