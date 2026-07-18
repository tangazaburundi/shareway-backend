package com.shareway.infrastructure.web.controller;

import com.shareway.application.dto.request.CreateReviewRequest;
import com.shareway.application.dto.response.ApiResponse;
import com.shareway.application.dto.response.PageResponse;
import com.shareway.application.dto.response.ReviewResponse;
import com.shareway.application.usecase.ReviewUseCase;
import com.shareway.infrastructure.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * ORDRE CRITIQUE : routes statiques AVANT /{id}
 * <p>
 * GET    /reviews/user/{userId}      → avis reçus par un user   ← statique AVANT /{id}
 * GET    /reviews/my                 → mes avis laissés          ← statique AVANT /{id}
 * GET    /reviews/can-review         → puis-je noter ?           ← statique AVANT /{id}
 * POST   /reviews                    → créer un avis
 * POST   /reviews/{id}/flag          → signaler
 */
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Avis et notation")
public class ReviewController {

    private final ReviewUseCase reviewUseCase;

    // ════════════════════════════════════════════════════════════════
    // Routes statiques — AVANT /{id}
    // ════════════════════════════════════════════════════════════════

    /**
     * GET /reviews/user/{userId}
     * Déclaré AVANT /{id}/flag etc. Pas de conflit car "user" ≠ UUID format.
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Avis reçus par un utilisateur")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getUserReviews(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                reviewUseCase.getForUser(userId, page, size)));
    }

    /**
     * GET /reviews/my
     * Route statique — AVANT /{id}.
     */
    @GetMapping("/my")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Mes avis laissés")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> myReviews() {
        return ResponseEntity.ok(ApiResponse.ok(
                reviewUseCase.getByAuthor(SecurityUtils.currentUserId())));
    }

    /**
     * GET /reviews/can-review?tripId=&targetId=
     * <p>
     * Route statique — AVANT /{id}.
     * Utilise des query params au lieu d'un path param pour éviter
     * que Spring confonde "/can-review/UUID" avec "/{id}".
     */
    @GetMapping("/can-review")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Puis-je encore noter cet utilisateur pour ce trajet ?",
            description = "Params: tripId (string), targetId (string). Retourne true/false."
    )
    public ResponseEntity<ApiResponse<Boolean>> canReview(
            @RequestParam String tripId,
            @RequestParam String targetId) {
        return ResponseEntity.ok(ApiResponse.ok(
                reviewUseCase.canReview(tripId, SecurityUtils.currentUserId(), targetId)));
    }

    // ════════════════════════════════════════════════════════════════
    // Routes sans path variable
    // ════════════════════════════════════════════════════════════════

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Laisser un avis (passager → conducteur OU conducteur → passager)",
            description = "Uniquement pour les trajets COMPLETED. Un seul avis par paire (trip × author × target)."
    )
    public ResponseEntity<ApiResponse<ReviewResponse>> create(
            @Valid @RequestBody CreateReviewRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        reviewUseCase.create(req, SecurityUtils.currentUserId()),
                        "Avis soumis avec succès"));
    }

    // ════════════════════════════════════════════════════════════════
    // Routes avec /{id} — toujours en dernier
    // ════════════════════════════════════════════════════════════════

    @PostMapping("/{id}/flag")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Signaler un avis inapproprié")
    public ResponseEntity<ApiResponse<Void>> flag(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        reviewUseCase.flagReview(id, body.getOrDefault("reason", ""), SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.noContent("Avis signalé"));
    }
}

