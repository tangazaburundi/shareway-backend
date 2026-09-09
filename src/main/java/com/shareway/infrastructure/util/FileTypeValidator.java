package com.shareway.infrastructure.util;

import com.shareway.domain.exception.InvalidOperationException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Valide le « vrai » type d'un fichier via ses magic bytes
 * (pas le Content-Type fourni par le client, qui est forgeable).
 * <p>
 * Types acceptés : JPEG, PNG, GIF, WEBP (images) et PDF (documents).
 */
public final class FileTypeValidator {

    private FileTypeValidator() {
    }

    public enum FileType {
        JPEG("image", "jpg"),
        PNG("image", "png"),
        GIF("image", "gif"),
        WEBP("image", "webp"),
        PDF("document", "pdf"),
        UNKNOWN(null, null);

        public final String category;
        public final String extension;

        FileType(String category, String extension) {
            this.category = category;
            this.extension = extension;
        }
    }

    /**
     * Analyse les magic bytes du fichier.
     *
     * @throws InvalidOperationException si le contenu est invalide ou illisible
     */
    public static FileType detect(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidOperationException("File is empty");
        }
        try (InputStream in = file.getInputStream()) {
            byte[] header = new byte[12];
            int read = in.readNBytes(header, 0, header.length);
            if (read < 4) {
                throw new InvalidOperationException("File content is invalid or too short");
            }
            return detect(header, read);
        } catch (IOException e) {
            throw new InvalidOperationException("Unable to read file content");
        }
    }

    private static FileType detect(byte[] h, int len) {
        // JPEG : FF D8 FF
        if (len >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
            return FileType.JPEG;
        }
        // PNG : 89 50 4E 47 0D 0A 1A 0A
        if (len >= 8
                && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G'
                && h[4] == 0x0D && h[5] == 0x0A && h[6] == 0x1A && h[7] == 0x0A) {
            return FileType.PNG;
        }
        // GIF : 'GIF8'
        if (len >= 4 && h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8') {
            return FileType.GIF;
        }
        // WEBP : 'RIFF' .... 'WEBP' (bytes 8..11)
        if (len >= 12
                && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') {
            return FileType.WEBP;
        }
        // PDF : '%PDF-'
        if (len >= 5 && h[0] == '%' && h[1] == 'P' && h[2] == 'D' && h[3] == 'F') {
            return FileType.PDF;
        }
        throw new InvalidOperationException("File type not allowed. Only JPG, PNG, GIF, WEBP and PDF are accepted");
    }
}