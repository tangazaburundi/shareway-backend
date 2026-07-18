package com.shareway.infrastructure.web.controller;

import com.shareway.application.dto.response.ApiResponse;
import com.shareway.domain.repository.SystemSettingRepository;
import com.shareway.domain.repository.TripRepository;
import com.shareway.domain.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
@Tag(name = "Public", description = "Endpoints publics (pas d'auth requise)")
public class PublicStatsController {

    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final SystemSettingRepository systemSettingRepository;

    @GetMapping("/stats")
    @Operation(summary = "Statistiques publiques pour la page d'accueil (flag admin)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPublicStats() {
        String flag = systemSettingRepository.findByKey("show_homepage_stats")
                .map(s -> s.getValue())
                .orElse("false");

        boolean visible = "true".equalsIgnoreCase(flag);

        Map<String, Object> data = new HashMap<>();
        data.put("visible", visible);

        if (visible) {
            data.put("totalUsers", userRepository.countActiveUsers());
            data.put("totalTrips", tripRepository.countOpen() + tripRepository.countAllCompleted());
            data.put("totalDrivers", userRepository.countDrivers());
        }

        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
