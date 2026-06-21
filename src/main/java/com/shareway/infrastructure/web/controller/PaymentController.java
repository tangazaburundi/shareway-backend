package com.shareway.infrastructure.web.controller;

import com.shareway.application.dto.response.ApiResponse;
import com.shareway.application.usecase.StripeUseCase;
import com.shareway.infrastructure.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Paiements Stripe")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final StripeUseCase stripeUseCase;

    @PostMapping("/intent/{bookingId}")
    @Operation(summary = "Créer un PaymentIntent Stripe pour une réservation")
    public ResponseEntity<ApiResponse<Map<String, String>>> createPaymentIntent(
            @PathVariable String bookingId) {
        return ResponseEntity.ok(ApiResponse.ok(
            stripeUseCase.createPaymentIntent(bookingId, SecurityUtils.currentUserId())));
    }

    @PostMapping("/webhook")
    @Operation(summary = "Webhook Stripe (ne pas appeler directement)")
    public ResponseEntity<Void> webhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sig) {
        stripeUseCase.handleWebhook(payload, sig);
        return ResponseEntity.ok().build();
    }
}
