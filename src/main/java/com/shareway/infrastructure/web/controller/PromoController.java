package com.shareway.infrastructure.web.controller;

import com.shareway.application.dto.response.ApiResponse;
import com.shareway.domain.model.PromoCode;
import com.shareway.domain.repository.PromoCodeRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/promo")
@RequiredArgsConstructor
@Tag(name = "Promo Codes", description = "Validation des codes promo")
public class PromoController {

    private final PromoCodeRepository promoCodeRepository;

    @GetMapping("/validate")
    @Operation(summary = "Valider un code promo")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validatePromo(@RequestParam String code) {
        return promoCodeRepository.findActiveByCode(code.toUpperCase().trim())
                .filter(PromoCode::isValid)
                .map(pc -> {
                    Map<String, Object> result = Map.of(
                            "valid", true,
                            "code", pc.getCode(),
                            "description", pc.getDescription(),
                            "discountType", pc.getDiscountType().name(),
                            "discountValue", pc.getDiscountValue(),
                            "maxDiscountAmount", pc.getMaxDiscountAmount() != null ? pc.getMaxDiscountAmount() : pc.getDiscountValue(),
                            "currency", pc.getCurrency() != null ? pc.getCurrency() : "FBU"
                    );
                    return ResponseEntity.ok(ApiResponse.ok(result, "Code promo valide"));
                })
                .orElse(ResponseEntity.ok(ApiResponse.ok(
                        Map.of("valid", false), "Code promo invalide ou expiré")));
    }
}
