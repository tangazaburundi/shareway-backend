package com.shareway.infrastructure.web.controller;

import com.shareway.application.dto.response.ApiResponse;
import com.shareway.application.dto.response.PartenaireResponse;
import com.shareway.application.usecase.PartenaireUseCase;
import com.shareway.infrastructure.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/partenaires")
@RequiredArgsConstructor
@Tag(name = "Partenaires", description = "Gestion des partenaires")
public class PartenaireController {

    private final PartenaireUseCase partenaireUseCase;

    @GetMapping("/active")
    @Operation(summary = "Tous les partenaires actifs (affichage public)")
    public ResponseEntity<ApiResponse<List<PartenaireResponse>>> getActive() {
        return ResponseEntity.ok(ApiResponse.ok(partenaireUseCase.getAllActive()));
    }

    @GetMapping("/admin")
    @Operation(summary = "Tous les partenaires (admin)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<PartenaireResponse>>> getAllForAdmin() {
        return ResponseEntity.ok(ApiResponse.ok(partenaireUseCase.getAllForAdmin()));
    }

    @GetMapping("/admin/{id}")
    @Operation(summary = "Détail d'un partenaire (admin)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PartenaireResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(partenaireUseCase.getById(id)));
    }

    @PostMapping("/admin")
    @Operation(summary = "Créer un partenaire")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PartenaireResponse>> create(@RequestBody Map<String, Object> body) {
        String nom = (String) body.get("nom");
        String imageUrl = (String) body.get("imageUrl");
        String lienUrl = (String) body.get("lienUrl");
        Integer sortOrder = body.get("sortOrder") != null ? ((Number) body.get("sortOrder")).intValue() : null;
        PartenaireResponse created = partenaireUseCase.create(nom, imageUrl, lienUrl, sortOrder,
                SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.ok(created, "Partenaire créé"));
    }

    @PutMapping("/admin/{id}")
    @Operation(summary = "Modifier un partenaire")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PartenaireResponse>> update(@PathVariable String id,
                                                                   @RequestBody Map<String, Object> body) {
        String nom = (String) body.get("nom");
        String imageUrl = (String) body.get("imageUrl");
        String lienUrl = (String) body.get("lienUrl");
        Boolean actif = body.get("actif") != null ? (Boolean) body.get("actif") : null;
        Integer sortOrder = body.get("sortOrder") != null ? ((Number) body.get("sortOrder")).intValue() : null;
        PartenaireResponse updated = partenaireUseCase.update(id, nom, imageUrl, lienUrl, actif, sortOrder,
                SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.ok(updated, "Partenaire mis à jour"));
    }

    @DeleteMapping("/admin/{id}")
    @Operation(summary = "Supprimer un partenaire")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        partenaireUseCase.delete(id, SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.noContent("Partenaire supprimé"));
    }

    @PostMapping(value = "/admin/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Uploader une image pour un partenaire")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> uploadImage(@PathVariable String id,
                                                           @RequestParam("file") MultipartFile file) {
        String url = partenaireUseCase.uploadImage(file);
        partenaireUseCase.update(id, null, url, null, null, null, SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.ok(url, "Image uploadée"));
    }
}
