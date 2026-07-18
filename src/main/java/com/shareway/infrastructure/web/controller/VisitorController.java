package com.shareway.infrastructure.web.controller;

import com.shareway.application.dto.request.RecordVisitRequest;
import com.shareway.application.dto.response.ApiResponse;
import com.shareway.application.dto.response.PageResponse;
import com.shareway.application.dto.response.VisitorRowResponse;
import com.shareway.application.dto.response.VisitorStatsResponse;
import com.shareway.application.usecase.VisitorUseCase;
import com.shareway.infrastructure.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Visitors", description = "Analytics et suivi des visiteurs")
public class VisitorController {

    private final VisitorUseCase visitorUseCase;

    @PostMapping("/visits")
    @Operation(summary = "Enregistrer une visite (public)")
    public ResponseEntity<ApiResponse<String>> recordVisit(
            @RequestBody RecordVisitRequest req,
            HttpServletRequest request) {

        String userId = null;
        try {
            userId = SecurityUtils.currentUserId();
        } catch (Exception ignored) {}

        String ip = getClientIp(request);
        visitorUseCase.recordVisit(req, userId, ip);

        return ResponseEntity.ok(ApiResponse.ok("Visit recorded"));
    }

    @PatchMapping("/visits/cookies")
    @Operation(summary = "Mettre à jour le consentement cookies")
    public ResponseEntity<ApiResponse<String>> updateCookies(
            @RequestParam String anonymousId,
            @RequestParam boolean accepted) {

        visitorUseCase.updateCookiesAccepted(anonymousId, accepted);
        return ResponseEntity.ok(ApiResponse.ok("Cookies updated"));
    }

    @GetMapping("/admin/visitors/stats")
    @Operation(summary = "Statistiques visiteurs pour admin")
    public ResponseEntity<ApiResponse<VisitorStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(visitorUseCase.getStats()));
    }

    @GetMapping("/admin/visitors")
    @Operation(summary = " Liste des visiteurs pour admin")
    public ResponseEntity<ApiResponse<PageResponse<VisitorRowResponse>>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) Boolean cookiesAccepted,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                visitorUseCase.getAllVisitors(search, country, cookiesAccepted, page, size)));
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
