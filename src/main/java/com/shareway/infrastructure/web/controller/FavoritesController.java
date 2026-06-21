package com.shareway.infrastructure.web.controller;

import com.shareway.application.dto.response.*;
import com.shareway.application.usecase.FavoritesUseCase;
import com.shareway.infrastructure.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Favorites & Blacklist", description = "Conducteurs favoris, liste noire")
@SecurityRequirement(name = "bearerAuth")
public class FavoritesController {

    private final FavoritesUseCase favoritesUseCase;

    // ===== FAVORITES =====
    @GetMapping("/favorites")
    @Operation(summary = "Mes conducteurs favoris")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getFavorites() {
        return ResponseEntity.ok(ApiResponse.ok(
            favoritesUseCase.getFavorites(SecurityUtils.currentUserId())));
    }

    @PostMapping("/favorites/{userId}")
    @Operation(summary = "Ajouter un conducteur en favori")
    public ResponseEntity<ApiResponse<Void>> addFavorite(@PathVariable String userId) {
        favoritesUseCase.addFavorite(SecurityUtils.currentUserId(), userId);
        return ResponseEntity.ok(ApiResponse.noContent("Added to favorites"));
    }

    @DeleteMapping("/favorites/{userId}")
    @Operation(summary = "Retirer un conducteur des favoris")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(@PathVariable String userId) {
        favoritesUseCase.removeFavorite(SecurityUtils.currentUserId(), userId);
        return ResponseEntity.ok(ApiResponse.noContent("Removed from favorites"));
    }

    @GetMapping("/favorites/{userId}/check")
    public ResponseEntity<ApiResponse<Boolean>> isFavorite(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.ok(
            favoritesUseCase.isFavorite(SecurityUtils.currentUserId(), userId)));
    }

    // ===== BLACKLIST =====
    @GetMapping("/blacklist")
    @Operation(summary = "Ma liste noire")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getBlacklist() {
        return ResponseEntity.ok(ApiResponse.ok(
            favoritesUseCase.getBlacklist(SecurityUtils.currentUserId())));
    }

    @PostMapping("/blacklist/{userId}")
    @Operation(summary = "Ajouter un utilisateur à ma liste noire")
    public ResponseEntity<ApiResponse<Void>> addToBlacklist(
            @PathVariable String userId, @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", "") : "";
        favoritesUseCase.addToBlacklist(SecurityUtils.currentUserId(), userId, reason);
        return ResponseEntity.ok(ApiResponse.noContent("User blacklisted"));
    }

    @DeleteMapping("/blacklist/{userId}")
    @Operation(summary = "Retirer un utilisateur de ma liste noire")
    public ResponseEntity<ApiResponse<Void>> removeFromBlacklist(@PathVariable String userId) {
        favoritesUseCase.removeFromBlacklist(SecurityUtils.currentUserId(), userId);
        return ResponseEntity.ok(ApiResponse.noContent("User removed from blacklist"));
    }
}
