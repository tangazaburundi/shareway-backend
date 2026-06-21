package com.shareway.infrastructure.adapter.storage;

import com.shareway.application.port.out.StoragePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

//TODO

/**
 * Stockage local (à remplacer par S3/MinIO en production).
 * Les fichiers sont servis via GET /static-files/** (FileController).
 * <p>
 * URL publique générée : {backendUrl}/api/v1/static-files/{folder}/{filename}
 */
@Slf4j
@Component
public class LocalStorageAdapter implements StoragePort {

    @Value("${shareway.app.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${shareway.app.backend-url:http://localhost:8080}")
    private String backendUrl;

    @Value("${server.servlet.context-path:/api/v1}")
    private String contextPath;

    @Override
    public String upload(MultipartFile file, String folder) {
        try {
            String ext = getExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
            Path dir = Paths.get(uploadDir, folder);

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

            Path file = Paths.get(uploadDir).resolve(relativePath).normalize();
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

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
