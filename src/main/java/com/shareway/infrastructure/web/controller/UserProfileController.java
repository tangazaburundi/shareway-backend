/*
package com.shareway.infrastructure.web.controller;

import com.shareway.application.dto.request.*;
import com.shareway.application.dto.response.*;
import com.shareway.application.usecase.*;
import com.shareway.infrastructure.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Profil utilisateur, avatar, mot de passe, 2FA")
@SecurityRequirement(name = "bearerAuth")
public class UserProfileController {

    private final UserProfileUseCase userProfileUseCase;

    @GetMapping
    @Operation(summary = "Mon profil complet")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile() {
        return ResponseEntity.ok(ApiResponse.ok(
            userProfileUseCase.getProfile(SecurityUtils.currentUserId())));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Profil public d'un autre utilisateur")
    public ResponseEntity<ApiResponse<UserResponse>> getUserProfile(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.ok(userProfileUseCase.getProfile(userId)));
    }

    @PutMapping
    @Operation(summary = "Mettre à jour mon profil")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
            userProfileUseCase.updateProfile(req, SecurityUtils.currentUserId()), "Profile updated"));
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Uploader ma photo de profil")
    public ResponseEntity<ApiResponse<String>> uploadAvatar(
            @RequestParam("file") MultipartFile file) {
        String url = userProfileUseCase.uploadAvatar(file, SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.ok(url, "Avatar uploaded"));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Changer mon mot de passe")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest req) {
        userProfileUseCase.changePassword(req, SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.noContent("Password changed successfully"));
    }

    // ===== 2FA =====
    @PostMapping("/2fa/setup")
    @Operation(summary = "Démarrer la configuration 2FA - retourne QR code")
    public ResponseEntity<ApiResponse<Map<String, String>>> setup2Fa() {
        return ResponseEntity.ok(ApiResponse.ok(
            userProfileUseCase.setup2Fa(SecurityUtils.currentUserId()), "Scan the QR code with your authenticator app"));
    }

    @PostMapping("/2fa/enable")
    @Operation(summary = "Activer la 2FA après vérification du code")
    public ResponseEntity<ApiResponse<Void>> enable2Fa(@Valid @RequestBody TwoFaVerifyRequest req) {
        userProfileUseCase.enable2Fa(req, SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.noContent("2FA enabled successfully"));
    }

    @PostMapping("/2fa/disable")
    @Operation(summary = "Désactiver la 2FA")
    public ResponseEntity<ApiResponse<Void>> disable2Fa(@Valid @RequestBody TwoFaVerifyRequest req) {
        userProfileUseCase.disable2Fa(req, SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.noContent("2FA disabled"));
    }
}
*/

package com.shareway.infrastructure.web.controller;

import com.shareway.application.dto.request.ChangePasswordRequest;
import com.shareway.application.dto.request.TwoFaVerifyRequest;
import com.shareway.application.dto.request.UpdateProfileRequest;
import com.shareway.application.dto.response.ApiResponse;
import com.shareway.application.dto.response.UserResponse;
import com.shareway.application.usecase.UserProfileUseCase;
import com.shareway.infrastructure.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * ORDRE CRITIQUE : toutes les routes statiques AVANT /{xxx}
 * <p>
 * GET  /profile              → mon profil
 * PUT  /profile              → mettre à jour
 * POST /profile/avatar       → upload photo    ← multipart/form-data
 * POST /profile/2fa/setup    → init 2FA        ← statique AVANT /{id}
 * POST /profile/2fa/enable   → activer 2FA     ← statique AVANT /{id}
 * POST /profile/2fa/disable  → désactiver 2FA  ← statique AVANT /{id}
 * GET  /profile/{userId}     → profil public   ← EN DERNIER
 */
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Profil utilisateur, avatar, mot de passe, 2FA")
@SecurityRequirement(name = "bearerAuth")
public class UserProfileController {

    private final UserProfileUseCase userProfileUseCase;

    // ════════════════════════════════════════════════════════════════
    // Routes statiques — AVANT /{userId}
    // ════════════════════════════════════════════════════════════════

    @GetMapping
    @Operation(summary = "Mon profil complet")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile() {
        return ResponseEntity.ok(ApiResponse.ok(
                userProfileUseCase.getProfile(SecurityUtils.currentUserId())));
    }

    @PutMapping
    @Operation(summary = "Mettre à jour mon profil")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                userProfileUseCase.updateProfile(req, SecurityUtils.currentUserId()),
                "Profil mis à jour"));
    }

    /**
     * POST /profile/avatar
     * Content-Type: multipart/form-data
     * Champ: file (image)
     * <p>
     * IMPORTANT : consumes = MULTIPART_FORM_DATA_VALUE
     * Si le front envoie application/json → erreur 415 claire grâce au GlobalExceptionHandler
     */
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Uploader mon avatar",
            description = "Content-Type OBLIGATOIRE: multipart/form-data. Champ: 'file'. Max 5MB. Formats: jpeg, png, webp."
    )
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadAvatar(
            @RequestParam("file") MultipartFile file) {
        String url = userProfileUseCase.uploadAvatar(file, SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("avatarUrl", url), "Avatar mis à jour"));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Changer mon mot de passe")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest req) {
        userProfileUseCase.changePassword(req, SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.noContent("Mot de passe modifié"));
    }

    // ── 2FA ──────────────────────────────────────────────────────────────

    @PostMapping("/2fa/setup")
    @Operation(summary = "Initier la configuration 2FA — retourne QR code base64")
    public ResponseEntity<ApiResponse<Map<String, String>>> setup2Fa() {
        return ResponseEntity.ok(ApiResponse.ok(
                userProfileUseCase.setup2Fa(SecurityUtils.currentUserId()),
                "Scannez le QR code avec votre application d'authentification"));
    }

    @PostMapping("/2fa/enable")
    @Operation(summary = "Activer la 2FA après vérification du code TOTP")
    public ResponseEntity<ApiResponse<Void>> enable2Fa(@Valid @RequestBody TwoFaVerifyRequest req) {
        userProfileUseCase.enable2Fa(req, SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.noContent("2FA activée avec succès"));
    }

    @PostMapping("/2fa/disable")
    @Operation(summary = "Désactiver la 2FA")
    public ResponseEntity<ApiResponse<Void>> disable2Fa(@Valid @RequestBody TwoFaVerifyRequest req) {
        userProfileUseCase.disable2Fa(req, SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.noContent("2FA désactivée"));
    }

    // ════════════════════════════════════════════════════════════════
    // Route avec {userId} — EN DERNIER
    // ════════════════════════════════════════════════════════════════

    @GetMapping("/{userId}")
    @Operation(summary = "Profil public d'un utilisateur")
    public ResponseEntity<ApiResponse<UserResponse>> getUserProfile(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.ok(userProfileUseCase.getProfile(userId)));
    }
}

