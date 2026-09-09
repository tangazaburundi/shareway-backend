package com.shareway.infrastructure.adapter.storage;

import com.shareway.application.port.out.StoragePort;
import com.shareway.domain.exception.InvalidOperationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Stockage local (à remplacer par S3/MinIO en production).
 * Les fichiers sont servis via GET /static-files/** (FileController).
 * <p>
 * Sécurité :
 * - le « folder » est validé (pas de path traversal, pas de « .. »)
 * - le chemin final est toujours normalisé et vérifié sous le root d'upload
 * - seules des extensions sûres sont autorisées
 * <p>
 * URL publique générée : {backendUrl}/api/v1/static-files/{folder}/{filename}
 */
@Slf4j
@Component
public class LocalStorageAdapter implements StoragePort {

    /** Extensions autorisées pour les fichiers servis publiquement. */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "gif", "pdf"
    );

    /** Caractères autorisés dans un folder : alphanum, tiret, underscore, slash. */
    private static final String FOLDER_PATTERN = "^[a-zA-Z0-9/_-]+$";

    @Value("${shareway.app.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${shareway.app.backend-url:http://localhost:8080}")
    private String backendUrl;

    @Value("${server.servlet.context-path:/api/v1}")
    private String contextPath;

    @Override
    public String upload(MultipartFile file, String folder) {
        if (!isSafeFolder(folder)) {
            log.warn("Rejected unsafe upload folder: {}", folder);
            throw new InvalidOperationException("Invalid upload folder");
        }

        try {
            String ext = getExtension(file.getOriginalFilename());
            if (!ALLOWED_EXTENSIONS.contains(ext)) {
                log.warn("Rejected upload with extension '{}' (name: {})", ext, file.getOriginalFilename());
                throw new InvalidOperationException("File type not allowed");
            }

            String filename = UUID.randomUUID() + "." + ext;

            Path uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path dir = uploadRoot.resolve(folder).normalize();
            if (!dir.startsWith(uploadRoot)) {
                log.warn("Rejected unsafe upload folder (path traversal): {}", folder);
                throw new InvalidOperationException("Invalid upload folder");
            }

            Files.createDirectories(dir);
            file.transferTo(dir.resolve(filename));

            String relativePath = folder + "/" + filename;
            String publicUrl = backendUrl + contextPath + "/static-files/" + relativePath;

            log.debug("File stored: {} → {}", relativePath, publicUrl);
            return publicUrl;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file in '" + folder + "'", e);
        }
    }

    @Override
    public void delete(String url) {
        try {
            // Extraire le chemin relatif depuis l'URL publique
            String marker = "/static-files/";
            int idx = url.indexOf(marker);
            if (idx < 0) return;
            String relativePath = url.substring(idx + marker.length());
            if (!isSafeFolder(relativePath)) {
                log.warn("Rejected unsafe delete path: {}", relativePath);
                return;
            }

            Path uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path file = uploadRoot.resolve(relativePath).normalize();
            if (!file.startsWith(uploadRoot)) {
                log.warn("Rejected unsafe delete path (traversal): {}", relativePath);
                return;
            }
            Files.deleteIfExists(file);
            log.debug("File deleted: {}", relativePath);

        } catch (IOException e) {
            log.warn("Could not delete file '{}': {}", url, e.getMessage());
        }
    }

    @Override
    public String getPublicUrl(String relativePath) {
        return backendUrl + contextPath + "/static-files/" + relativePath;
    }

    /**
     * Vérifie qu'un folder/chemin relatif ne contient pas de « .. »
     * ni de caractères dangereux.
     */
    private boolean isSafeFolder(String folder) {
        if (folder == null || folder.isBlank()) return false;
        if (folder.contains("..")) return false;
        if (folder.startsWith("/") || folder.startsWith("\\")) return false;
        if (folder.matches("^[a-zA-Z]:.*")) return false;
        return folder.matches(FOLDER_PATTERN);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
