/*
package com.shareway.infrastructure.web.controller;

import com.shareway.application.dto.request.BookTripRequest;
import com.shareway.application.dto.request.CreateTripRequest;
import com.shareway.application.dto.request.TripSearchRequest;
import com.shareway.application.dto.response.ApiResponse;
import com.shareway.application.dto.response.PageResponse;
import com.shareway.application.dto.response.TripResponse;
import com.shareway.application.usecase.TripUseCase;
import com.shareway.infrastructure.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

*/
/**
 * Routes trips :
 * <p>
 * GET    /trips                    Recherche (departureCity seul suffit)
 * POST   /trips                    Créer un trajet (conducteur)
 * GET    /trips/my                 Mes trajets (conducteur)
 * GET    /trips/my/bookings        Mes réservations (passager)
 * GET    /trips/share/{token}      Trajet par token de partage   ← AVANT /{id}
 * GET    /trips/{id}               Détail d'un trajet
 * POST   /trips/{id}/join          Rejoindre (passager)
 * POST   /trips/{id}/cancel        Annuler (conducteur)
 * PATCH  /trips/{id}/complete      Terminer (conducteur)
 * <p>
 * IMPORTANT : les routes statiques (/my, /share/…) sont TOUTES déclarées
 * avant /{id} pour éviter que Spring MVC les confonde avec un UUID.
 * <p>
 * GET /trips/share/{token}
 * <p>
 * DOIT être déclaré avant GET /trips/{id} sinon Spring MVC tente de
 * traiter "share" comme un UUID → NoResourceFoundException.
 * <p>
 * GET /trips/share/{token}
 * <p>
 * DOIT être déclaré avant GET /trips/{id} sinon Spring MVC tente de
 * traiter "share" comme un UUID → NoResourceFoundException.
 * <p>
 * GET /trips/share/{token}
 * <p>
 * DOIT être déclaré avant GET /trips/{id} sinon Spring MVC tente de
 * traiter "share" comme un UUID → NoResourceFoundException.
 * <p>
 * GET /trips/share/{token}
 * <p>
 * DOIT être déclaré avant GET /trips/{id} sinon Spring MVC tente de
 * traiter "share" comme un UUID → NoResourceFoundException.
 * <p>
 * GET /trips/share/{token}
 * <p>
 * DOIT être déclaré avant GET /trips/{id} sinon Spring MVC tente de
 * traiter "share" comme un UUID → NoResourceFoundException.
 * <p>
 * GET /trips/share/{token}
 * <p>
 * DOIT être déclaré avant GET /trips/{id} sinon Spring MVC tente de
 * traiter "share" comme un UUID → NoResourceFoundException.
 * <p>
 * GET /trips/share/{token}
 * <p>
 * DOIT être déclaré avant GET /trips/{id} sinon Spring MVC tente de
 * traiter "share" comme un UUID → NoResourceFoundException.
 * <p>
 * GET /trips/share/{token}
 * <p>
 * DOIT être déclaré avant GET /trips/{id} sinon Spring MVC tente de
 * traiter "share" comme un UUID → NoResourceFoundException.
 * <p>
 * GET /trips/share/{token}
 * <p>
 * DOIT être déclaré avant GET /trips/{id} sinon Spring MVC tente de
 * traiter "share" comme un UUID → NoResourceFoundException.
 * <p>
 * GET /trips/share/{token}
 * <p>
 * DOIT être déclaré avant GET /trips/{id} sinon Spring MVC tente de
 * traiter "share" comme un UUID → NoResourceFoundException.
 * <p>
 * GET /trips/share/{token}
 * <p>
 * DOIT être déclaré avant GET /trips/{id} sinon Spring MVC tente de
 * traiter "share" comme un UUID → NoResourceFoundException.
 * <p>
 * GET /trips/share/{token}
 * <p>
 * DOIT être déclaré avant GET /trips/{id} sinon Spring MVC tente de
 * traiter "share" comme un UUID → NoResourceFoundException.
 * <p>
 * GET /trips/share/{token}
 * <p>
 * DOIT être déclaré avant GET /trips/{id} sinon Spring MVC tente de
 * traiter "share" comme un UUID → NoResourceFoundException.
 * <p>
 * GET /trips/share/{token}
 * <p>
 * DOIT être déclaré avant GET /trips/{id} sinon Spring MVC tente de
 * traiter "share" comme un UUID → NoResourceFoundException.
 * <p>
 * GET /trips/share/{token}
 * <p>
 * DOIT être déclaré avant GET /trips/{id} sinon Spring MVC tente de
 * traiter "share" comme un UUID → NoResourceFoundException.
 * <p>
 * GET /trips/share/{token}
 * <p>
 * DOIT être déclaré avant GET /trips/{id} sinon Spring MVC tente de
 * traiter "share" comme un UUID → NoResourceFoundException.
 * <p>
 * GET /trips/share/{token}
 * <p>
 * DOIT être déclaré avant GET /trips/{id} sinon Spring MVC tente de
 * traiter "share" comme un UUID → NoResourceFoundException.
 *//*

@RestController
@RequestMapping("/trips")
@RequiredArgsConstructor
@Tag(name = "Trips", description = "Gestion des trajets")
public class TripController {

    private final TripUseCase tripUseCase;

    // ── Routes sans paramètre de chemin ──────────────────────────────────

    @GetMapping
    @Operation(
            summary = "Rechercher des trajets",
            description = "Seul departureCity est obligatoire. " +
                    "Filtres optionnels : arrivalCity, date, seats, maxPrice, minRating, " +
                    "pets, smallLuggage, largeLuggage, airConditioning."
    )
    public ResponseEntity<ApiResponse<PageResponse<TripResponse>>> search(
            @ModelAttribute TripSearchRequest req,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(tripUseCase.search(req, page, size)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Créer un trajet (rôle DRIVER ou BOTH requis)")
    public ResponseEntity<ApiResponse<TripResponse>> create(
            @Valid @RequestBody CreateTripRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        tripUseCase.create(req, SecurityUtils.currentUserId()), "Trip created"));
    }

    // ── Routes statiques — AVANT /{id} ───────────────────────────────────

    @GetMapping("/my")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Mes trajets créés (conducteur)")
    public ResponseEntity<ApiResponse<List<TripResponse>>> myTrips() {
        return ResponseEntity.ok(ApiResponse.ok(
                tripUseCase.getMyTrips(SecurityUtils.currentUserId())));
    }

    @GetMapping("/my/bookings")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Mes réservations (passager)")
    public ResponseEntity<ApiResponse<List<TripResponse>>> myBookings() {
        return ResponseEntity.ok(ApiResponse.ok(
                tripUseCase.getMyBookings(SecurityUtils.currentUserId())));
    }

    */
/**
 * GET /trips/share/{token}
 * <p>
 * DOIT être déclaré avant GET /trips/{id} sinon Spring MVC tente de
 * traiter "share" comme un UUID → NoResourceFoundException.
 *//*

    @GetMapping("/share/{token}")
    @Operation(summary = "Trajet par token de partage (lien partageable)")
    public ResponseEntity<ApiResponse<TripResponse>> getByShareToken(
            @PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.ok(tripUseCase.getByShareToken(token)));
    }

    // ── Routes avec {id} — TOUJOURS en dernier ───────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un trajet")
    public ResponseEntity<ApiResponse<TripResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(tripUseCase.getById(id)));
    }

    @PostMapping("/{id}/join")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Rejoindre un trajet (passager)")
    public ResponseEntity<ApiResponse<Void>> join(
            @PathVariable String id,
            @Valid @RequestBody BookTripRequest req) {
        tripUseCase.book(id, req, SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.noContent("Booking confirmed"));
    }

    @PostMapping("/{id}/cancel")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Annuler un trajet (conducteur uniquement)")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        tripUseCase.cancel(id, body.getOrDefault("reason", ""), SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.noContent("Trip cancelled"));
    }

    @PatchMapping("/{id}/complete")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Terminer un trajet (conducteur)",
            description = "Marque le trajet comme COMPLETED. " +
                    "Envoie automatiquement une notification aux passagers pour les inviter à noter."
    )
    public ResponseEntity<ApiResponse<Void>> complete(@PathVariable String id) {
        tripUseCase.complete(id, SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.noContent("Trip completed"));
    }

    @PostMapping("/{id}/leave")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Annuler un trajet (passager uniquement)")
    public ResponseEntity<ApiResponse<Void>> leaveTrip(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(tripUseCase.leaveTrip(id, body.getOrDefault("reason", ""), SecurityUtils.currentUserId())));
    }

}
*/

package com.shareway.infrastructure.web.controller;

import com.shareway.application.dto.request.BookTripRequest;
import com.shareway.application.dto.request.CreateTripRequest;
import com.shareway.application.dto.request.RespondBookingRequest;
import com.shareway.application.dto.request.TripSearchRequest;
import com.shareway.application.dto.request.UpdateTripRequest;
import com.shareway.application.dto.response.ApiResponse;
import com.shareway.application.dto.response.BookingResponse;
import com.shareway.application.dto.response.PageResponse;
import com.shareway.application.dto.response.PassengerPublicResponse;
import com.shareway.application.dto.response.TripEditHistoryResponse;
import com.shareway.application.dto.response.TripResponse;
import com.shareway.application.usecase.TripUseCase;
import com.shareway.infrastructure.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * ORDRE CRITIQUE des routes :
 *   routes statiques (/my, /my/bookings, /share/…) AVANT /{id}
 *   sous-routes /{id}/bookings, /{id}/leave… APRÈS
 *
 * Toutes les routes "/{id}/quelquechose" doivent être déclarées
 * explicitement ici — Spring ne peut pas les deviner.
 */
@RestController
@RequestMapping("/trips")
@RequiredArgsConstructor
@Tag(name = "Trips", description = "Gestion des trajets")
public class TripController {

    private final TripUseCase tripUseCase;

    // ════════════════════════════════════════════════════════════════
    // Routes sans {id} — déclarées en premier
    // ════════════════════════════════════════════════════════════════

    @GetMapping
    @Operation(summary = "Rechercher des trajets (departureCity seul suffit)")
    public ResponseEntity<ApiResponse<PageResponse<TripResponse>>> search(
            @ModelAttribute TripSearchRequest req,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(tripUseCase.search(req, page, size)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Créer un trajet")
    public ResponseEntity<ApiResponse<TripResponse>> create(@Valid @RequestBody CreateTripRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(tripUseCase.create(req, SecurityUtils.currentUserId()), "Trajet créé"));
    }

    // ════════════════════════════════════════════════════════════════
    // Routes statiques — AVANT /{id}
    // ════════════════════════════════════════════════════════════════

    @GetMapping("/my")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Mes trajets créés (conducteur)")
    public ResponseEntity<ApiResponse<List<TripResponse>>> myTrips() {
        return ResponseEntity.ok(ApiResponse.ok(
                tripUseCase.getMyTrips(SecurityUtils.currentUserId())));
    }

    @GetMapping("/my/bookings")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Mes réservations (passager)")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> myBookings() {
        return ResponseEntity.ok(ApiResponse.ok(
                tripUseCase.getMyBookings(SecurityUtils.currentUserId())));
    }

    /** Doit être AVANT /{id} — sinon "share" est pris comme UUID */
    @GetMapping("/share/{token}")
    @Operation(summary = "Trajet par token de partage")
    public ResponseEntity<ApiResponse<TripResponse>> getByShareToken(@PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.ok(tripUseCase.getByShareToken(token)));
    }

    // ════════════════════════════════════════════════════════════════
    // Routes avec /{id} — toujours en dernier
    // ════════════════════════════════════════════════════════════════

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un trajet")
    public ResponseEntity<ApiResponse<TripResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(tripUseCase.getById(id)));
    }

/*
    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Modifier un trajet (conducteur)",
            description = "Tous les champs sont optionnels. Les passagers confirmés reçoivent une notification si des champs importants changent."
    )
    public ResponseEntity<ApiResponse<TripResponse>> update(
            @PathVariable String id, @Valid @RequestBody UpdateTripRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                tripUseCase.update(id, req, SecurityUtils.currentUserId()), "Trajet modifié"));
    }
*/


    // ── MODIFICATION DU TRAJET ─────────────────────────────────────────

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Modifier un trajet publié (conducteur)",
            description = """
                    Modifie un trajet existant. Tous les champs sont optionnels — seuls les champs
                    fournis et non-null sont mis à jour (patch sémantique).
                     
                    **Règles :**
                    - Seul le conducteur propriétaire peut modifier
                    - Impossible si statut = COMPLETED ou CANCELLED
                    - totalSeats ne peut pas descendre sous le nombre de réservations actives
                    - departureTime doit être dans le futur
                     
                    **Notifications :**
                    Si ville de départ/arrivée, heure ou prix changent, tous les passagers
                    CONFIRMED/PENDING reçoivent une notification push automatique.
                    Le champ `notificationMessage` permet de personnaliser ce message.
                     
                    **Historique :**
                    Chaque champ modifié est enregistré dans `trip_edit_history` (consultable
                    via `GET /trips/{id}/history`).
                    """
    )
    public ResponseEntity<ApiResponse<TripResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateTripRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                tripUseCase.update(id, req, SecurityUtils.currentUserId()),
                "Trajet modifié avec succès"));
    }

    @PostMapping("/{id}/join")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Demander à rejoindre un trajet (passager)",
            description = "Crée une réservation en statut PENDING. Le conducteur doit accepter ou refuser."
    )
    public ResponseEntity<ApiResponse<BookingResponse>> join(
            @PathVariable String id, @Valid @RequestBody BookTripRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        tripUseCase.book(id, req, SecurityUtils.currentUserId()),
                        "Demande envoyée, en attente de validation du conducteur"));
    }

    @PostMapping("/{id}/leave")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Annuler sa réservation (passager)",
            description = "Possible même si la réservation est PENDING ou CONFIRMED."
    )
    public ResponseEntity<ApiResponse<Void>> leave(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", "") : "";
        tripUseCase.leaveTrip(id, SecurityUtils.currentUserId(), reason);
        return ResponseEntity.ok(ApiResponse.noContent("Réservation annulée"));
    }

    @PostMapping("/{id}/cancel")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Annuler le trajet (conducteur uniquement)")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        tripUseCase.cancel(id, body.getOrDefault("reason", ""), SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.noContent("Trajet annulé"));
    }

    @PatchMapping("/{id}/complete")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Terminer un trajet (conducteur)")
    public ResponseEntity<ApiResponse<Void>> complete(@PathVariable String id) {
        tripUseCase.complete(id, SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.noContent("Trajet terminé"));
    }

    // ── Réservations du trajet (conducteur) ────────────────────────
    @GetMapping("/{id}/bookings")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Réservations d'un trajet (conducteur)",
            description = "Liste toutes les demandes de réservation avec statut PENDING/CONFIRMED/REJECTED."
    )
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getTripBookingsByIdTrip(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(
                tripUseCase.getTripBookings(id, SecurityUtils.currentUserId())));
    }

    @GetMapping("/bookings")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Tous les réservations pour un conducteur",
            description = "Liste toutes les demandes de réservation."
    )
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getAllTripBookings() {
        return ResponseEntity.ok(ApiResponse.ok(
                tripUseCase.getTripBookingsByIdDriver(SecurityUtils.currentUserId())));
    }

    @PostMapping("/bookings/{bookingId}/respond")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Accepter ou refuser une réservation (conducteur)",
            description = "body: { action: 'ACCEPTED' | 'REJECTED', reason?: string }"
    )
    public ResponseEntity<ApiResponse<BookingResponse>> respondToBooking(
            @PathVariable String bookingId,
            @Valid @RequestBody RespondBookingRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                tripUseCase.respondToBooking(bookingId, req, SecurityUtils.currentUserId()),
                "ACCEPTED".equals(req.getAction()) ? "Réservation acceptée" : "Réservation refusée"));
    }

    // ── Profil passager (visible par le conducteur du trajet) ──────
    @GetMapping("/{id}/passengers/{passengerId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Profil d'un passager (conducteur uniquement)",
            description = "Permet au conducteur de consulter le profil et les avis d'un passager qui a réservé."
    )
    public ResponseEntity<ApiResponse<PassengerPublicResponse>> getPassengerProfile(
            @PathVariable String id,
            @PathVariable String passengerId) {
        return ResponseEntity.ok(ApiResponse.ok(
                tripUseCase.getPassengerProfile(id, passengerId, SecurityUtils.currentUserId())));
    }


    // ── HISTORIQUE DES MODIFICATIONS ──────────────────────────────────

    @GetMapping("/{id}/history")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Historique des modifications du trajet (conducteur)",
            description = "Retourne la liste de tous les changements apportés au trajet, avec les anciennes et nouvelles valeurs, et le nombre de passagers notifiés."
    )
    public ResponseEntity<ApiResponse<List<TripEditHistoryResponse>>> getHistory(
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(
                tripUseCase.getHistory(id, SecurityUtils.currentUserId())));
    }

}

