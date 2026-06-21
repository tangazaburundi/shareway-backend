package com.shareway.infrastructure.web.controller;

import com.shareway.application.dto.response.*;
import com.shareway.application.usecase.DocumentUseCase;
import com.shareway.infrastructure.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Gestion documentaire (permis, carte grise…)")
@SecurityRequirement(name = "bearerAuth")
public class DocumentController {

    private final DocumentUseCase documentUseCase;

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Uploader un document (permis, carte grise…)")
    public ResponseEntity<ApiResponse<DocumentResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type,
            @RequestParam(value = "expiresAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiresAt) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(documentUseCase.uploadDocument(file, type, expiresAt,
                SecurityUtils.currentUserId()), "Document uploaded, pending review"));
    }

    @GetMapping("/documents")
    @Operation(summary = "Mes documents")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> myDocuments() {
        return ResponseEntity.ok(ApiResponse.ok(
            documentUseCase.getMyDocuments(SecurityUtils.currentUserId())));
    }

    // Admin endpoints
    @GetMapping("/admin/documents/pending")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','MODERATOR')")
    @Operation(summary = "Admin - Documents en attente de validation")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> pendingDocuments() {
        return ResponseEntity.ok(ApiResponse.ok(documentUseCase.getPendingDocuments()));
    }

    @PostMapping("/admin/documents/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','MODERATOR')")
    @Operation(summary = "Admin - Approuver un document")
    public ResponseEntity<ApiResponse<DocumentResponse>> approve(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(
            documentUseCase.approveDocument(id, SecurityUtils.currentUserId()), "Document approved"));
    }

    @PostMapping("/admin/documents/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','MODERATOR')")
    @Operation(summary = "Admin - Rejeter un document")
    public ResponseEntity<ApiResponse<DocumentResponse>> reject(
            @PathVariable String id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok(
            documentUseCase.rejectDocument(id, body.get("reason"), SecurityUtils.currentUserId()),
            "Document rejected"));
    }
}
