package com.shareway.infrastructure.web.controller;

import com.shareway.application.dto.response.ApiResponse;
import com.shareway.application.usecase.ReferralUseCase;
import com.shareway.infrastructure.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/referrals")
@RequiredArgsConstructor
@Tag(name = "Referrals", description = "Système de parrainage")
public class ReferralController {

    private final ReferralUseCase referralUseCase;

    @PostMapping("/generate")
    @Operation(summary = "Générer un code de parrainage")
    public ResponseEntity<ApiResponse<Map<String, String>>> generateReferralCode() {
        String code = referralUseCase.generateReferralCode(SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("referralCode", code), "Code de parrainage généré"));
    }

    @GetMapping("/stats")
    @Operation(summary = "Statistiques de parrainage")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReferralStats() {
        return ResponseEntity.ok(ApiResponse.ok(referralUseCase.getReferralStats(SecurityUtils.currentUserId())));
    }
}
