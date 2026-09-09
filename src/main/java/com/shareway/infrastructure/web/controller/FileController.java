package com.shareway.infrastructure.web.controller;

import com.shareway.domain.exception.NotAuthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

/**
 * Sert les fichiers uploadés (avatars, documents…).
 * <p>
 * IMPORTANT : ce controller est monté sous /static-files/** (et NON pas sous /**)
 * pour éviter tout conflit avec les autres routes REST (ex: /messages/conversation/**).
 * <p>
 * Contrôle d'accès :
 * - avatars/**        → public (lecture)
 * - documents/**      → propriétaire du document OU admin (lecture)
 * <p>
 * Accès : GET /static-files/avatars/userId/photo.jpg
 * GET /static-files/documents/userId/identity/doc.pdf
 */
@Slf4j
@RestController
@RequestMapping("/static-files")
public class FileController {

    private static final Set<String> ADMIN_ROLES = Set.of("ADMIN", "SUPER_ADMIN", "MODERATOR");

    @Value("${shareway.app.upload-dir:./uploads}")
    private String uploadDir;

    @GetMapping("/**")
    public ResponseEntity<Resource> serveFile(HttpServletRequest request) {
        // Extraire le chemin relatif après /static-files/
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String servletPath = requestUri.substring(contextPath.length());

        // Retirer le préfixe /api/v1/static-files/
        String relativePath = servletPath.replaceFirst("^.*/static-files/", "");

        try {
            Path uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path targetFile = uploadRoot.resolve(relativePath).normalize();

            // Sécurité : empêcher path traversal (../../etc/passwd)
            if (!targetFile.startsWith(uploadRoot)) {
                log.warn("Path traversal attempt blocked: {}", relativePath);
                return ResponseEntity.badRequest().build();
            }

            // Sécurité : les documents sensibles ne sont accessibles
            // qu'au propriétaire ou à un admin
            if (relativePath.startsWith("documents/") && !isOwnerOrAdmin(relativePath)) {
                throw new NotAuthorizedException("Not authorized to access this document");
            }

            Resource resource = new UrlResource(targetFile.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(targetFile);
            MediaType mediaType = contentType != null
                    ? MediaType.parseMediaType(contentType)
                    : MediaType.APPLICATION_OCTET_STREAM;

            boolean isDocument = relativePath.startsWith("documents/");
            String filename = targetFile.getFileName() != null ? targetFile.getFileName().toString() : "file";

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    // Documents sensibles : téléchargement forcé (pas de rendu inline)
                    // + pas de cache (le statut de validation peut changer).
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            (isDocument ? "attachment" : "inline") + "; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, isDocument ? "no-store" : "max-age=86400, public")
                    .header("X-Content-Type-Options", "nosniff")
                    .body(resource);

        } catch (IOException e) {
            log.error("Error serving file '{}': {}", relativePath, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Vérifie que l'utilisateur authentifié est le propriétaire du document
     * (dossier « documents/{userId}/… ») ou un administrateur.
     */
    private boolean isOwnerOrAdmin(String relativePath) {
        String[] parts = relativePath.split("/");
        if (parts.length < 2) return false;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;

        // Admin ? (autorités ROLE_ADMIN / ROLE_SUPER_ADMIN / ROLE_MODERATOR)
        boolean isAdmin = auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .anyMatch(authName -> ADMIN_ROLES.contains(authName.replace("ROLE_", "")));
        if (isAdmin) return true;

        // Propriétaire ? (principal = userId dans le token JWT)
        String ownerId = parts[1];
        return ownerId != null && ownerId.equals(auth.getPrincipal());
    }
}
