package com.shareway.infrastructure.web.controller;

import com.shareway.application.dto.response.ApiResponse;
import com.shareway.application.usecase.DashboardUseCase;
import com.shareway.infrastructure.security.SecurityUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Tableaux de bord conducteur et passager")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardUseCase dashboardUseCase;

    @GetMapping("/driver")
    public ResponseEntity<ApiResponse<DashboardUseCase.DriverDashboard>> driverDashboard() {
        return ResponseEntity.ok(ApiResponse.ok(
            dashboardUseCase.getDriverDashboard(SecurityUtils.currentUserId())));
    }

    @GetMapping("/passenger")
    public ResponseEntity<ApiResponse<DashboardUseCase.PassengerDashboard>> passengerDashboard() {
        return ResponseEntity.ok(ApiResponse.ok(
            dashboardUseCase.getPassengerDashboard(SecurityUtils.currentUserId())));
    }
}
