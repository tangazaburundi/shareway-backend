package com.shareway.infrastructure.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Sert les fichiers uploadés (avatars, documents…).
 * <p>
 * IMPORTANT : ce controller est monté sous /static-files/** (et NON pas sous /**)
 * pour éviter tout conflit avec les autres routes REST (ex: /messages/conversation/**).
 * <p>
 * Accès : GET /static-files/avatars/userId/photo.jpg
 * GET /static-files/documents/userId/identity/doc.pdf
 */
@Slf4j
@RestController
@RequestMapping("/static-files")
public class FileController {

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

            Resource resource = new UrlResource(targetFile.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(targetFile);
            MediaType mediaType = contentType != null
                    ? MediaType.parseMediaType(contentType)
                    : MediaType.APPLICATION_OCTET_STREAM;

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=86400, public")
                    .body(resource);

        } catch (IOException e) {
            log.error("Error serving file '{}': {}", relativePath, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
