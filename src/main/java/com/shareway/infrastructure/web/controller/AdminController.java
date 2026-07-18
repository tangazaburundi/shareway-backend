package com.shareway.infrastructure.web.controller;

import com.shareway.application.dto.request.AdminBlockUserRequest;
import com.shareway.application.dto.request.AdminReviewReportRequest;
import com.shareway.application.dto.response.ApiResponse;
import com.shareway.application.dto.response.AuditLogResponse;
import com.shareway.application.dto.response.MessageResponse;
import com.shareway.application.dto.response.PageResponse;
import com.shareway.application.dto.response.ReportResponse;
import com.shareway.application.dto.response.ReviewResponse;
import com.shareway.application.dto.response.TripResponse;
import com.shareway.application.dto.response.UserResponse;
import com.shareway.application.usecase.AdminUseCase;
import com.shareway.domain.model.RoleRequest;
import com.shareway.infrastructure.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Administration avancée - accès restreint")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','MODERATOR')")
public class AdminController {

    private final AdminUseCase adminUseCase;

    // ===== DASHBOARD =====
    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard admin - statistiques globales temps réel")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard() {
        return ResponseEntity.ok(ApiResponse.ok(adminUseCase.getDashboard()));
    }

    // ===== USERS =====
    @GetMapping("/users")
    @Operation(summary = "Liste de tous les utilisateurs avec recherche")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.ok(adminUseCase.getUsers(page, size, search)));
    }

    @PostMapping("/users/{id}/block")
    @Operation(summary = "Bloquer un utilisateur")
    public ResponseEntity<ApiResponse<UserResponse>> blockUser(
            @PathVariable String id, @Valid @RequestBody AdminBlockUserRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminUseCase.blockUser(id, req, SecurityUtils.currentUserId()), "User blocked"));
    }

    @PostMapping("/users/{id}/unblock")
    @Operation(summary = "Débloquer un utilisateur")
    public ResponseEntity<ApiResponse<UserResponse>> unblockUser(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminUseCase.unblockUser(id, SecurityUtils.currentUserId()), "User unblocked"));
    }

    @PostMapping("/users/{id}/verify-identity")
    @Operation(summary = "Valider l'identité d'un utilisateur")
    public ResponseEntity<ApiResponse<UserResponse>> verifyIdentity(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminUseCase.verifyIdentity(id, SecurityUtils.currentUserId()), "Identity verified"));
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Supprimer définitivement un utilisateur (soft delete)")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String id) {
        adminUseCase.deleteUser(id, SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.noContent("User deleted"));
    }

    // ===== MODERATION AVIS =====
    @GetMapping("/reviews/flagged")
    @Operation(summary = "Avis signalés en attente de modération")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> flaggedReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(adminUseCase.getFlaggedReviews(page, size)));
    }

    @PostMapping("/reviews/{id}/approve")
    @Operation(summary = "Approuver un avis signalé")
    public ResponseEntity<ApiResponse<ReviewResponse>> approveReview(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminUseCase.approveReview(id, SecurityUtils.currentUserId()), "Review approved"));
    }

    @PostMapping("/reviews/{id}/reject")
    @Operation(summary = "Rejeter/masquer un avis signalé")
    public ResponseEntity<ApiResponse<ReviewResponse>> rejectReview(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminUseCase.rejectReview(id, SecurityUtils.currentUserId()), "Review rejected"));
    }

    // ===== MODERATION MESSAGES =====
    @GetMapping("/messages/flagged")
    @Operation(summary = "Messages signalés")
    public ResponseEntity<ApiResponse<PageResponse<MessageResponse>>> flaggedMessages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(adminUseCase.getFlaggedMessages(page, size)));
    }

    // ===== SIGNALEMENTS =====
    @GetMapping("/reports")
    @Operation(summary = "Liste des signalements avec filtre par statut")
    public ResponseEntity<ApiResponse<PageResponse<ReportResponse>>> getReports(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(adminUseCase.getReports(status, page, size)));
    }

    @PostMapping("/reports/{id}/review")
    @Operation(summary = "Traiter un signalement")
    public ResponseEntity<ApiResponse<ReportResponse>> reviewReport(
            @PathVariable String id, @Valid @RequestBody AdminReviewReportRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminUseCase.reviewReport(id, req, SecurityUtils.currentUserId()), "Report reviewed"));
    }

    // ===== EXPORTS =====
    @GetMapping(value = "/export/users/csv", produces = "text/csv")
    @Operation(summary = "Exporter les utilisateurs en CSV")
    public ResponseEntity<byte[]> exportUsersCsv() {
        byte[] data = adminUseCase.exportUsersCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(data);
    }

    @GetMapping(value = "/export/users/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Operation(summary = "Exporter les utilisateurs en Excel")
    public ResponseEntity<byte[]> exportUsersExcel() {
        byte[] data = adminUseCase.exportUsersExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    // ===== AUDIT LOGS =====
    @GetMapping("/audit")
    @Operation(summary = "Journal d'audit complet")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getAuditLogs(
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.ok(adminUseCase.getAuditLogs(userId, page, size)));
    }

    // ===== APPROBATION / REJET UTILISATEURS =====
    @PostMapping("/users/{id}/approve")
    @Operation(summary = "Approuver un compte utilisateur")
    public ResponseEntity<ApiResponse<UserResponse>> approveUser(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminUseCase.approveUser(id, SecurityUtils.currentUserId()), "User approved"));
    }

    @PostMapping("/users/{id}/reject")
    @Operation(summary = "Rejeter un compte utilisateur")
    public ResponseEntity<ApiResponse<UserResponse>> rejectUser(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(ApiResponse.ok(
                adminUseCase.rejectUser(id, SecurityUtils.currentUserId(), reason), "User rejected"));
    }

    @PutMapping("/users/{id}/role")
    @Operation(summary = "Changer le rôle d'un utilisateur")
    public ResponseEntity<ApiResponse<UserResponse>> changeUserRole(
            @PathVariable String id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminUseCase.changeUserRole(id, body.get("role"), SecurityUtils.currentUserId()), "Role updated"));
    }

    // ===== DEMANDES DE RÔLE =====
    @GetMapping("/role-requests")
    @Operation(summary = "Liste des demandes de rôle avec filtre")
    public ResponseEntity<ApiResponse<PageResponse<RoleRequest>>> getRoleRequests(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(adminUseCase.getRoleRequests(status, page, size)));
    }

    @GetMapping("/role-requests/user/{userId}")
    @Operation(summary = "Demandes de rôle d'un utilisateur")
    public ResponseEntity<ApiResponse<List<RoleRequest>>> getMyRoleRequests(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.ok(adminUseCase.getMyRoleRequests(userId)));
    }

    @PostMapping("/role-requests/{id}/approve")
    @Operation(summary = "Approuver une demande de rôle")
    public ResponseEntity<ApiResponse<RoleRequest>> approveRoleRequest(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        String comment = body != null ? body.get("comment") : null;
        return ResponseEntity.ok(ApiResponse.ok(
                adminUseCase.approveRoleRequest(id, SecurityUtils.currentUserId(), comment), "Role request approved"));
    }

    @PostMapping("/role-requests/{id}/reject")
    @Operation(summary = "Rejeter une demande de rôle")
    public ResponseEntity<ApiResponse<RoleRequest>> rejectRoleRequest(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        String comment = body != null ? body.get("comment") : null;
        return ResponseEntity.ok(ApiResponse.ok(
                adminUseCase.rejectRoleRequest(id, SecurityUtils.currentUserId(), comment), "Role request rejected"));
    }

    // ===== GESTION DES VOYAGES =====
    @GetMapping("/trips")
    @Operation(summary = "Liste de tous les voyages avec filtre par statut")
    public ResponseEntity<ApiResponse<PageResponse<TripResponse>>> getAllTrips(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(adminUseCase.getAllTrips(status, page, size)));
    }

    @PostMapping("/trips/{id}/approve")
    @Operation(summary = "Approuver un voyage")
    public ResponseEntity<ApiResponse<TripResponse>> approveTrip(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminUseCase.approveTrip(id, SecurityUtils.currentUserId()), "Trip approved"));
    }

    @PostMapping("/trips/{id}/reject")
    @Operation(summary = "Rejeter un voyage")
    public ResponseEntity<ApiResponse<TripResponse>> rejectTrip(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(ApiResponse.ok(
                adminUseCase.rejectTrip(id, SecurityUtils.currentUserId(), reason), "Trip rejected"));
    }

    @PostMapping("/settings/{key}")
    @Operation(summary = "Modifier un paramètre système")
    public ResponseEntity<ApiResponse<Void>> updateSetting(
            @PathVariable String key,
            @RequestBody Map<String, String> body) {
        adminUseCase.updateSystemSetting(key, body.get("value"));
        return ResponseEntity.ok(ApiResponse.noContent("Setting updated"));
    }
}
