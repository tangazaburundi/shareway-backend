package com.shareway.infrastructure.web.controller;

import com.shareway.application.dto.request.CreateRideRequest;
import com.shareway.application.dto.request.RateRideRequest;
import com.shareway.application.dto.request.RespondRideRequest;
import com.shareway.application.dto.request.UpdateDriverLocationRequest;
import com.shareway.application.dto.response.ApiResponse;
import com.shareway.application.dto.response.DriverAvailabilityResponse;
import com.shareway.application.dto.response.NearbyDriverResponse;
import com.shareway.application.dto.response.PricingConfigResponse;
import com.shareway.application.dto.response.RideEstimateResponse;
import com.shareway.application.dto.response.RideRatingResponse;
import com.shareway.application.dto.response.RideResponse;
import com.shareway.application.dto.response.SmsConfigResponse;
import com.shareway.application.usecase.RideUseCase;
import com.shareway.infrastructure.security.SecurityUtils;
import com.shareway.domain.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rides")
@RequiredArgsConstructor
@Tag(name = "Rides (On-Demand)", description = "Mode Uber — courses à la demande")
public class RideController {

    private final RideUseCase rideUseCase;
    private final InvoiceService invoiceService;

    // ════════════════════════════════════════════════════════════════
    // PUBLIC
    // ════════════════════════════════════════════════════════════════

    @GetMapping("/estimate")
    @Operation(summary = "Estimation de prix pour une course")
    public ResponseEntity<ApiResponse<RideEstimateResponse>> getEstimate(
            @RequestParam BigDecimal pickupLat,
            @RequestParam BigDecimal pickupLng,
            @RequestParam BigDecimal destinationLat,
            @RequestParam BigDecimal destinationLng,
            @RequestParam(defaultValue = "FBU") String currency) {
        return ResponseEntity.ok(ApiResponse.ok(
                rideUseCase.getEstimate(pickupLat, pickupLng, destinationLat, destinationLng, currency)));
    }

    @GetMapping("/nearby")
    @Operation(summary = "Chauffeurs proches d'une position (debug/admin)")
    public ResponseEntity<ApiResponse<List<NearbyDriverResponse>>> getNearbyDrivers(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") int max) {
        return ResponseEntity.ok(ApiResponse.ok(rideUseCase.getNearbyDrivers(lat, lng, max)));
    }

    @GetMapping("/config/timeout")
    @Operation(summary = "Timeout de recherche en secondes (public)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSearchTimeout() {
        return ResponseEntity.ok(ApiResponse.ok(rideUseCase.getSearchTimeoutConfig()));
    }

    // ════════════════════════════════════════════════════════════════
    // PASSENGER
    // ════════════════════════════════════════════════════════════════

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Demander une course (passager)")
    public ResponseEntity<ApiResponse<RideResponse>> createRide(
            @Valid @RequestBody CreateRideRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        rideUseCase.createRideRequest(req, SecurityUtils.currentUserId()),
                        "Recherche de chauffeur en cours..."));
    }

    @GetMapping("/my-active")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Course active du passager")
    public ResponseEntity<ApiResponse<RideResponse>> getMyActiveRide() {
        return rideUseCase.getActiveRide(SecurityUtils.currentUserId())
                .map(r -> ResponseEntity.ok(ApiResponse.ok(r)))
                .orElse(ResponseEntity.ok(ApiResponse.ok(null, "Aucune course active")));
    }

    @GetMapping("/my-history")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Historique des courses du passager")
    public ResponseEntity<ApiResponse<List<RideResponse>>> getMyHistory() {
        return ResponseEntity.ok(ApiResponse.ok(
                rideUseCase.getPassengerHistory(SecurityUtils.currentUserId())));
    }

    @GetMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Détail d'une course")
    public ResponseEntity<ApiResponse<RideResponse>> getRideById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(
                rideUseCase.getRideById(id, SecurityUtils.currentUserId())));
    }

    @PostMapping("/{id}/cancel")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Annuler une course (passager)")
    public ResponseEntity<ApiResponse<Void>> cancelRide(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", "") : "";
        rideUseCase.cancelRide(id, reason, SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.noContent("Course annulée"));
    }

    @PostMapping("/{id}/rate")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Noter une course")
    public ResponseEntity<ApiResponse<RideRatingResponse>> rateRide(
            @PathVariable String id,
            @Valid @RequestBody RateRideRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        rideUseCase.rateRide(id, req, SecurityUtils.currentUserId()),
                        "Merci pour votre avis !"));
    }

    @GetMapping("/ratings/user/{userId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Avis taxi reçus par un utilisateur")
    public ResponseEntity<ApiResponse<List<RideRatingResponse>>> getRideRatingsForUser(
            @PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.ok(
                rideUseCase.getRideRatingsForUser(userId)));
    }

    // ════════════════════════════════════════════════════════════════
    // CHAUFFEUR
    // ════════════════════════════════════════════════════════════════

    @PostMapping("/{id}/accept")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Accepter une course (chauffeur)")
    public ResponseEntity<ApiResponse<RideResponse>> acceptRide(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(
                rideUseCase.acceptRide(id, SecurityUtils.currentUserId()),
                "Course acceptée !"));
    }

    @PostMapping("/{id}/reject")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Refuser une course (chauffeur)")
    public ResponseEntity<ApiResponse<Void>> rejectRide(
            @PathVariable String id,
            @RequestBody(required = false) java.util.Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        rideUseCase.rejectRide(id, SecurityUtils.currentUserId(), reason);
        return ResponseEntity.ok(ApiResponse.noContent("Course refusée"));
    }

    @PostMapping("/{id}/timeout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Timeout — le chauffeur n'a pas répondu à temps")
    public ResponseEntity<ApiResponse<Void>> timeoutRide(@PathVariable String id) {
        rideUseCase.timeoutRide(id, SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.noContent("Timeout traité"));
    }

    @PostMapping("/{id}/driver-en-route")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Chauffeur en route vers le passager")
    public ResponseEntity<ApiResponse<RideResponse>> driverEnRoute(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(
                rideUseCase.driverEnRoute(id, SecurityUtils.currentUserId())));
    }

    @PostMapping("/{id}/driver-arrived")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Chauffeur arrivé au point de pick-up")
    public ResponseEntity<ApiResponse<RideResponse>> driverArrived(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(
                rideUseCase.driverArrived(id, SecurityUtils.currentUserId())));
    }

    @PostMapping("/{id}/start")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Démarrer la course")
    public ResponseEntity<ApiResponse<RideResponse>> startRide(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(
                rideUseCase.startRide(id, SecurityUtils.currentUserId())));
    }

    @PostMapping("/{id}/complete")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Terminer la course")
    public ResponseEntity<ApiResponse<RideResponse>> completeRide(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(
                rideUseCase.completeRide(id, SecurityUtils.currentUserId()),
                "Course terminée !"));
    }

    @PostMapping("/{id}/driver-cancel")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Annuler la course (chauffeur)")
    public ResponseEntity<ApiResponse<Void>> driverCancelRide(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", "") : "";
        rideUseCase.driverCancelRide(id, reason, SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.noContent("Course annulée"));
    }

    @PostMapping("/{id}/transfer")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Rendre/transférer la course au chauffeur le plus proche")
    public ResponseEntity<ApiResponse<RideResponse>> transferRide(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(
                rideUseCase.transferRide(id, SecurityUtils.currentUserId()),
                "Course transférée"));
    }

    // ════════════════════════════════════════════════════════════════
    // FACTURE — Génération PDF
    // ════════════════════════════════════════════════════════════════

    @GetMapping("/{id}/invoice")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Générer la facture PDF d'une course terminée")
    public ResponseEntity<byte[]> generateInvoice(@PathVariable String id) {
        RideResponse ride = rideUseCase.getRideById(id, SecurityUtils.currentUserId());
        byte[] pdf = invoiceService.generateInvoice(ride);
        String filename = "facture-SW-" + id.substring(0, Math.min(8, id.length())).toUpperCase() + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }

    @GetMapping("/{id}/receipt")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Générer le ticket de caisse (80mm) d'une course terminée")
    public ResponseEntity<byte[]> generateReceipt(@PathVariable String id) {
        RideResponse ride = rideUseCase.getRideById(id, SecurityUtils.currentUserId());
        byte[] pdf = invoiceService.generateReceipt(ride);
        String filename = "ticket-SW-" + id.substring(0, Math.min(8, id.length())).toUpperCase() + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }

    // ════════════════════════════════════════════════════════════════
    // CHAUFFEUR — Disponibilité & Position
    // ════════════════════════════════════════════════════════════════

    @PutMapping("/driver/availability")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Toggle disponibilité (ON/OFF)")
    public ResponseEntity<ApiResponse<com.shareway.application.dto.response.DriverAvailabilityResponse>>
    toggleAvailability() {
        return ResponseEntity.ok(ApiResponse.ok(
                rideUseCase.toggleAvailability(SecurityUtils.currentUserId())));
    }

    @GetMapping("/driver/availability")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Statut de disponibilité actuel")
    public ResponseEntity<ApiResponse<com.shareway.application.dto.response.DriverAvailabilityResponse>>
    getAvailability() {
        return ResponseEntity.ok(ApiResponse.ok(
                rideUseCase.getDriverAvailability(SecurityUtils.currentUserId())));
    }

    @PutMapping("/driver/location")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Mettre à jour la position GPS du chauffeur")
    public ResponseEntity<ApiResponse<Void>> updateLocation(
            @Valid @RequestBody UpdateDriverLocationRequest req) {
        rideUseCase.updateDriverLocation(SecurityUtils.currentUserId(), req);
        return ResponseEntity.ok(ApiResponse.noContent("Position mise à jour"));
    }

    @GetMapping("/driver/active")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Course active du chauffeur")
    public ResponseEntity<ApiResponse<RideResponse>> getDriverActiveRide() {
        return rideUseCase.getDriverActiveRide(SecurityUtils.currentUserId())
                .map(r -> ResponseEntity.ok(ApiResponse.ok(r)))
                .orElse(ResponseEntity.ok(ApiResponse.ok(null, "Aucune course active")));
    }

    @GetMapping("/driver/history")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Historique des courses du chauffeur")
    public ResponseEntity<ApiResponse<List<RideResponse>>> getDriverHistory() {
        return ResponseEntity.ok(ApiResponse.ok(
                rideUseCase.getDriverHistory(SecurityUtils.currentUserId())));
    }

    // ════════════════════════════════════════════════════════════════
    // ADMIN — Pricing Config
    // ════════════════════════════════════════════════════════════════

    @GetMapping("/admin/pricing-config")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Lister les configs tarifaires")
    public ResponseEntity<ApiResponse<List<PricingConfigResponse>>> getAllPricingConfigs() {
        return ResponseEntity.ok(ApiResponse.ok(rideUseCase.getAllPricingConfigs()));
    }

    @GetMapping("/admin/pricing-config/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Détail d'une config tarifaire")
    public ResponseEntity<ApiResponse<PricingConfigResponse>> getPricingConfig(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(rideUseCase.getPricingConfig(id)));
    }

    @PostMapping("/admin/pricing-config")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Créer une config tarifaire")
    public ResponseEntity<ApiResponse<PricingConfigResponse>> createPricingConfig(
            @Valid @RequestBody PricingConfigResponse req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(rideUseCase.createPricingConfig(req)));
    }

    @PutMapping("/admin/pricing-config/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Modifier une config tarifaire")
    public ResponseEntity<ApiResponse<PricingConfigResponse>> updatePricingConfig(
            @PathVariable String id,
            @Valid @RequestBody PricingConfigResponse req) {
        return ResponseEntity.ok(ApiResponse.ok(rideUseCase.updatePricingConfig(id, req)));
    }

    @DeleteMapping("/admin/pricing-config/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Supprimer une config tarifaire")
    public ResponseEntity<ApiResponse<Void>> deletePricingConfig(@PathVariable String id) {
        rideUseCase.deletePricingConfig(id);
        return ResponseEntity.ok(ApiResponse.noContent("Config tarifaire supprimée"));
    }

    // ════════════════════════════════════════════════════════════════
    // ADMIN — SMS Config
    // ════════════════════════════════════════════════════════════════

    @GetMapping("/admin/sms-config")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Config SMS actuelle")
    public ResponseEntity<ApiResponse<SmsConfigResponse>> getSmsConfig() {
        return ResponseEntity.ok(ApiResponse.ok(rideUseCase.getSmsConfig()));
    }

    @PutMapping("/admin/sms-config")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Modifier la config SMS")
    public ResponseEntity<ApiResponse<SmsConfigResponse>> updateSmsConfig(
            @Valid @RequestBody SmsConfigResponse req) {
        return ResponseEntity.ok(ApiResponse.ok(rideUseCase.updateSmsConfig(req)));
    }

    // ════════════════════════════════════════════════════════════════
    // SOS — Alerte d'urgence
    // ════════════════════════════════════════════════════════════════

    @PostMapping("/{id}/sos")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Déclencher une alerte SOS pendant une course")
    public ResponseEntity<ApiResponse<Void>> sosAlert(@PathVariable String id) {
        rideUseCase.triggerSosAlert(id, SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.noContent("Alerte SOS envoyée"));
    }

    // ════════════════════════════════════════════════════════════════
    // CHAT — Messagerie in-ride
    // ════════════════════════════════════════════════════════════════

    @GetMapping("/{id}/messages")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Messages d'une course")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRideMessages(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(rideUseCase.getRideMessages(id, SecurityUtils.currentUserId())));
    }

    @PostMapping("/{id}/messages")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Envoyer un message dans une course")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendRideMessage(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        String content = body.getOrDefault("content", "");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(rideUseCase.sendRideMessage(id, content, SecurityUtils.currentUserId())));
    }

    // ════════════════════════════════════════════════════════════════
    // EARNINGS — Statistiques chauffeur
    // ════════════════════════════════════════════════════════════════

    @GetMapping("/driver/earnings")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Revenus du chauffeur (jour/semaine/mois)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDriverEarnings() {
        return ResponseEntity.ok(ApiResponse.ok(
                rideUseCase.getDriverEarnings(SecurityUtils.currentUserId())));
    }

    @GetMapping("/driver/earnings/weekly")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Revenus hebdomadaires du chauffeur")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDriverEarningsWeekly() {
        return ResponseEntity.ok(ApiResponse.ok(
                rideUseCase.getDriverEarningsWeekly(SecurityUtils.currentUserId())));
    }

    // ════════════════════════════════════════════════════════════════
    // PROMO — Codes promo
    // ════════════════════════════════════════════════════════════════

    @PostMapping("/{id}/apply-promo")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Appliquer un code promo à une course")
    public ResponseEntity<ApiResponse<RideResponse>> applyPromoCode(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        String code = body.getOrDefault("code", "");
        return ResponseEntity.ok(ApiResponse.ok(
                rideUseCase.applyPromoCode(id, code, SecurityUtils.currentUserId())));
    }
}
