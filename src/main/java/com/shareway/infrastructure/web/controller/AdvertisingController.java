package com.shareway.infrastructure.web.controller;

import com.shareway.application.dto.request.CreateAdvertisingRequest;
import com.shareway.application.dto.request.UpdateAdvertisingRequest;
import com.shareway.application.dto.response.AdvertisingResponse;
import com.shareway.application.dto.response.ApiResponse;
import com.shareway.application.dto.response.PageResponse;
import com.shareway.application.usecase.AdvertisingUseCase;
import com.shareway.infrastructure.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Advertising", description = "Gestion dynamique des publicités")
public class AdvertisingController {

    private final AdvertisingUseCase advertisingUseCase;

    // ─── Endpoints publics ─────────────────────────────────────────────────

    @GetMapping("/public/ads")
    @Operation(summary = "Toutes les publicités actives (affichage public)")
    public ResponseEntity<ApiResponse<List<AdvertisingResponse>>> getActiveAds() {
        return ResponseEntity.ok(ApiResponse.ok(advertisingUseCase.getAllActive()));
    }

    @GetMapping("/public/ads/{position}")
    @Operation(summary = "Publicités actives filtrées par position")
    public ResponseEntity<ApiResponse<List<AdvertisingResponse>>> getActiveAdsByPosition(@PathVariable String position) {
        return ResponseEntity.ok(ApiResponse.ok(advertisingUseCase.getActiveByPosition(position)));
    }

    @PostMapping("/public/ads/{id}/click")
    @Operation(summary = "Enregistrer un clic sur une publicité")
    public ResponseEntity<ApiResponse<Void>> recordClick(@PathVariable String id) {
        advertisingUseCase.recordClick(id);
        return ResponseEntity.ok(ApiResponse.noContent("Click recorded"));
    }

    // ─── Endpoints admin ────────────────────────────────────────────────────

    @PostMapping("/admin/ads")
    @Operation(summary = "Créer une nouvelle publicité")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AdvertisingResponse>> create(@Valid @RequestBody CreateAdvertisingRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                advertisingUseCase.create(req, SecurityUtils.currentUserId()), "Publicité créée"));
    }

    @GetMapping("/admin/ads")
    @Operation(summary = "Liste paginée de toutes les publicités (admin)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','MODERATOR')")
    public ResponseEntity<ApiResponse<PageResponse<AdvertisingResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AdvertisingResponse> result = advertisingUseCase.getAll(page, size);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result)));
    }

    @GetMapping("/admin/ads/{id}")
    @Operation(summary = "Détail d'une publicité (admin)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','MODERATOR')")
    public ResponseEntity<ApiResponse<AdvertisingResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(advertisingUseCase.getById(id)));
    }

    @PutMapping("/admin/ads/{id}")
    @Operation(summary = "Modifier une publicité")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AdvertisingResponse>> update(
            @PathVariable String id, @Valid @RequestBody UpdateAdvertisingRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                advertisingUseCase.update(id, req, SecurityUtils.currentUserId()), "Publicité mise à jour"));
    }

    @DeleteMapping("/admin/ads/{id}")
    @Operation(summary = "Supprimer une publicité")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        advertisingUseCase.delete(id, SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.noContent("Publicité supprimée"));
    }

    @PostMapping(value = "/admin/ads/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Uploader une image publicitaire")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = advertisingUseCase.uploadImage(file);
        return ResponseEntity.ok(ApiResponse.ok(url, "Image uploadée"));
    }

    @PostMapping("/admin/ads/{id}/activate")
    @Operation(summary = "Activer une publicité")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AdvertisingResponse>> activate(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(
                advertisingUseCase.activate(id, SecurityUtils.currentUserId()), "Publicité activée"));
    }

    @PostMapping("/admin/ads/{id}/deactivate")
    @Operation(summary = "Désactiver une publicité")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AdvertisingResponse>> deactivate(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(
                advertisingUseCase.deactivate(id, SecurityUtils.currentUserId()), "Publicité désactivée"));
    }

    @GetMapping("/admin/ads/stats")
    @Operation(summary = "Statistiques des publicités")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(advertisingUseCase.getStats()));
    }
}
