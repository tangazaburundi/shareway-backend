package com.shareway.infrastructure.web.controller;

import com.shareway.application.dto.request.CreateRoleRequest;
import com.shareway.application.dto.request.SaveVehicleRequest;
import com.shareway.application.dto.request.SwitchRoleRequest;
import com.shareway.application.dto.request.UpdateUserProfileRequest;
import com.shareway.application.dto.response.ApiResponse;
import com.shareway.application.dto.response.DashboardStatsResponse;
import com.shareway.application.dto.response.NotificationResponse;
import com.shareway.application.dto.response.RoleRequestResponse;
import com.shareway.application.dto.response.UserResponse;
import com.shareway.application.dto.response.VehicleResponse;
import com.shareway.application.usecase.UserUseCase;
import com.shareway.infrastructure.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Controller mappé sur /users/**
 * <p>
 * Correspond exactement au UserService Angular :
 * <p>
 * GET    /users/{id}                    → getProfile(id)
 * GET    /users/me                      → getMe()
 * PUT    /users/me                      → updateProfile(data)
 * POST   /users/me/avatar              → uploadAvatar(file)
 * POST   /users/me/identity            → uploadIdentity(file)
 * PUT    /users/me/vehicle             → saveVehicle(vehicle)
 * PATCH  /users/me/role                → switchRole(role)
 * GET    /users/me/notifications       → getNotifications()
 * PATCH  /users/me/notifications/read  → markNotificationsRead(ids)
 * GET    /users/me/stats               → getDashboardStats()
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Profil, véhicule, notifications, statistiques")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserUseCase userUseCase;

    // ──────────────────────────────────────────────────────────────────────
    // GET /users/{id}
    // Profil public d'un utilisateur (accessible même sans être l'owner)
    // ──────────────────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    @Operation(
            summary = "Profil public d'un utilisateur",
            description = "Retourne les informations publiques. Les champs privés (email, token 2FA…) sont masqués."
    )
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(userUseCase.getProfile(id)));
    }

    // ──────────────────────────────────────────────────────────────────────
    // GET /users/me
    // Mon profil complet (tous champs, y compris privés)
    // ──────────────────────────────────────────────────────────────────────
    @GetMapping("/me")
    @Operation(
            summary = "Mon profil complet",
            description = "Retourne toutes les données de l'utilisateur connecté, y compris email, 2FA, blockReason…"
    )
    public ResponseEntity<ApiResponse<UserResponse>> getMe() {
        return ResponseEntity.ok(ApiResponse.ok(
                userUseCase.getMe(SecurityUtils.currentUserId())));
    }


    // ──────────────────────────────────────────────────────────────────────
    // PUT /users/me
    // Mise à jour du profil (Partial<User> → tous les champs optionnels)
    // ──────────────────────────────────────────────────────────────────────
    @PutMapping("/me")
    @Operation(
            summary = "Mettre à jour mon profil",
            description = "Tous les champs sont optionnels. Seuls les champs fournis sont mis à jour."
    )
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateUserProfileRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                userUseCase.updateProfile(req, SecurityUtils.currentUserId()),
                "Profile updated successfully"));
    }


    // ──────────────────────────────────────────────────────────────────────
    // POST /users/me/avatar  (multipart/form-data)
    // Retourne { avatarUrl: string } comme attendu par le frontend
    // ──────────────────────────────────────────────────────────────────────
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Uploader mon avatar",
            description = "Accepte image/jpeg, image/png, image/webp. Max 5 MB. Retourne l'URL publique."
    )
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadAvatar(
            @RequestParam("file") MultipartFile file) {
        String url = userUseCase.uploadAvatar(file, SecurityUtils.currentUserId());
        // Réponse = { avatarUrl: "https://..." } comme dans l'interface Angular
        return ResponseEntity.ok(ApiResponse.ok(Map.of("avatarUrl", url), "Avatar uploaded"));
    }

    // ──────────────────────────────────────────────────────────────────────
    // POST /users/me/identity  (multipart/form-data)
    // Upload d'une pièce d'identité → statut PENDING, validé par un admin
    // ──────────────────────────────────────────────────────────────────────

    @PostMapping(value = "/me/identity", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Uploader ma pièce d'identité (multipart/form-data)")
    public ResponseEntity<ApiResponse<Void>> uploadIdentity(
            @RequestParam("file") MultipartFile file) {
        userUseCase.uploadIdentity(file, SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.noContent("Document soumis, en attente de validation"));
    }

    // ──────────────────────────────────────────────────────────────────────
    // PUT /users/me/vehicle
    // Crée ou met à jour le véhicule principal du conducteur
    // ──────────────────────────────────────────────────────────────────────
    @PutMapping("/me/vehicle")
    @Operation(
            summary = "Enregistrer / mettre à jour mon véhicule",
            description = "Si un véhicule actif existe déjà, il est mis à jour. Sinon un nouveau est créé. " +
                    "Nécessite le rôle DRIVER ou BOTH."
    )
    public ResponseEntity<ApiResponse<VehicleResponse>> saveVehicle(
            @Valid @RequestBody SaveVehicleRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                userUseCase.saveVehicle(req, SecurityUtils.currentUserId()), "Véhicule enregistré"));
    }

    // ──────────────────────────────────────────────────────────────────────
    // PATCH /users/me/role
    // Changer de rôle : DRIVER ↔ PASSENGER ↔ BOTH
    // ──────────────────────────────────────────────────────────────────────
    @PatchMapping("/me/role")
    @Operation(
            summary = "Changer mon rôle",
            description = "Passer de DRIVER à PASSENGER nécessite de n'avoir aucun trajet OPEN en cours."
    )
    public ResponseEntity<ApiResponse<UserResponse>> switchRole(
            @Valid @RequestBody SwitchRoleRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                userUseCase.switchRole(req.getRole(), SecurityUtils.currentUserId()),
                "Rôle mis à jour : " + req.getRole()));
    }

    // ──────────────────────────────────────────────────────────────────────
    // POST /users/me/role-requests
    // Demander un changement de rôle (validé par un admin)
    // ──────────────────────────────────────────────────────────────────────
    @PostMapping("/me/role-requests")
    @Operation(
            summary = "Demander un changement de rôle",
            description = "Soumet une demande de changement de rôle (DRIVER, PASSENGER ou BOTH). En attente de validation admin."
    )
    public ResponseEntity<ApiResponse<RoleRequestResponse>> createRoleRequest(
            @Valid @RequestBody CreateRoleRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                userUseCase.createRoleRequest(req.getRequestedRole(), req.getReason(), SecurityUtils.currentUserId()),
                "Demande de rôle soumise, en attente de validation"));
    }

    // ──────────────────────────────────────────────────────────────────────
    // GET /users/me/role-requests
    // Historique de mes demandes de changement de rôle
    // ──────────────────────────────────────────────────────────────────────
    @GetMapping("/me/role-requests")
    @Operation(
            summary = "Mes demandes de rôle",
            description = "Retourne l'historique des demandes de changement de rôle de l'utilisateur connecté."
    )
    public ResponseEntity<ApiResponse<List<RoleRequestResponse>>> getMyRoleRequests() {
        return ResponseEntity.ok(ApiResponse.ok(
                userUseCase.getMyRoleRequests(SecurityUtils.currentUserId())));
    }

    // ──────────────────────────────────────────────────────────────────────
    // GET /users/me/notifications
    // Liste des 50 dernières notifications (triées par date desc)
    // ──────────────────────────────────────────────────────────────────────
    @GetMapping("/me/notifications")
    @Operation(
            summary = "Mes notifications",
            description = "Retourne les 50 dernières notifications, lues et non lues."
    )
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications() {
        return ResponseEntity.ok(ApiResponse.ok(
                userUseCase.getNotifications(SecurityUtils.currentUserId())));
    }

    // ──────────────────────────────────────────────────────────────────────
    // PATCH /users/me/notifications/read
    // Body : { ids: string[] }  →  si ids vide/null, toutes sont marquées lues
    // ──────────────────────────────────────────────────────────────────────
    @PatchMapping("/me/notifications/read")
    @Operation(
            summary = "Marquer des notifications comme lues",
            description = "Passer ids=[\"id1\",\"id2\"] pour cibler des notifs. " +
                    "Passer ids=[] ou omettre ids pour tout marquer comme lu."
    )
    public ResponseEntity<ApiResponse<Void>> markNotificationsRead(
            @RequestBody(required = false) Map<String, List<String>> body) {
        List<String> ids = body != null ? body.get("ids") : null;
        userUseCase.markNotificationsRead(ids, SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.noContent("Notifications marquées comme lues"));
    }

    // ──────────────────────────────────────────────────────────────────────
    // GET /users/me/stats
    // Statistiques du tableau de bord (conducteur ET passager)
    // ──────────────────────────────────────────────────────────────────────
    @GetMapping("/me/stats")
    @Operation(
            summary = "Mes statistiques de tableau de bord",
            description = "Retourne totalTrips, totalPassengers, totalEarnings, earningsByCurrency, " +
                    "rating, reviewCount, completionRate, upcomingTrips, cancelledTrips, monthlyEarnings."
    )
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        return ResponseEntity.ok(ApiResponse.ok(
                userUseCase.getDashboardStats(SecurityUtils.currentUserId())));
    }
}
