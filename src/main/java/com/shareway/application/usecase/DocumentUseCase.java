package com.shareway.application.usecase;

import com.shareway.application.dto.response.DocumentResponse;
import com.shareway.application.port.out.AuditPort;
import com.shareway.application.port.out.NotificationPort;
import com.shareway.application.port.out.StoragePort;
import com.shareway.domain.exception.InvalidOperationException;
import com.shareway.domain.exception.UserNotFoundException;
import com.shareway.domain.model.User;
import com.shareway.domain.model.UserDocument;
import com.shareway.domain.repository.UserDocumentRepository;
import com.shareway.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentUseCase {

    private final UserDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final StoragePort storagePort;
    private final NotificationPort notificationPort;
    private final AuditPort auditPort;

    public DocumentResponse uploadDocument(MultipartFile file, String type, LocalDate expiresAt, String userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (file.isEmpty()) throw new InvalidOperationException("File is empty");
        long maxSize = 10L * 1024 * 1024; // 10MB
        if (file.getSize() > maxSize) throw new InvalidOperationException("File too large (max 10MB)");

        String url = storagePort.upload(file, "documents/" + userId + "/" + type.toLowerCase());

        UserDocument doc = UserDocument.builder()
                .user(user)
                .type(UserDocument.DocumentType.valueOf(type))
                .fileUrl(url)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .expiresAt(expiresAt)
                .status(UserDocument.DocumentStatus.PENDING)
                .build();

        documentRepository.save(doc);
        auditPort.log("DOCUMENT_UPLOADED", "UserDocument", doc.getId(), null, type, userId);
        return toResponse(doc);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getMyDocuments(String userId) {
        return documentRepository.findByUserIdAndDeletedAtIsNull(userId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    // Admin: approve a document
    public DocumentResponse approveDocument(String docId, String adminId) {
        UserDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new InvalidOperationException("Document not found"));
        doc.setStatus(UserDocument.DocumentStatus.APPROVED);
        doc.setReviewedAt(java.time.LocalDateTime.now());
        doc.setReviewedBy(adminId);
        documentRepository.save(doc);

        notificationPort.notify(doc.getUser().getId(), "IDENTITY_VERIFIED",
                "Document approved", "Your " + doc.getType().name() + " has been approved.");
        auditPort.log("DOCUMENT_APPROVED", "UserDocument", docId, null, null, adminId);
        return toResponse(doc);
    }

    // Admin: reject a document
    public DocumentResponse rejectDocument(String docId, String reason, String adminId) {
        UserDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new InvalidOperationException("Document not found"));
        doc.setStatus(UserDocument.DocumentStatus.REJECTED);
        doc.setRejectionReason(reason);
        doc.setReviewedAt(java.time.LocalDateTime.now());
        doc.setReviewedBy(adminId);
        documentRepository.save(doc);

        notificationPort.notify(doc.getUser().getId(), "SYSTEM",
                "Document rejected", "Your " + doc.getType().name() + " was rejected: " + reason);
        auditPort.log("DOCUMENT_REJECTED", "UserDocument", docId, null, reason, adminId);
        return toResponse(doc);
    }

    // Admin: list all pending documents
    @Transactional(readOnly = true)
    public List<DocumentResponse> getPendingDocuments() {
        return documentRepository.findByStatus(UserDocument.DocumentStatus.PENDING).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    private DocumentResponse toResponse(UserDocument d) {
        return DocumentResponse.builder()
                .id(d.getId()).type(d.getType().name()).fileUrl(d.getFileUrl())
                .fileName(d.getFileName()).status(d.getStatus().name())
                .rejectionReason(d.getRejectionReason()).expiresAt(d.getExpiresAt())
                .createdAt(d.getCreatedAt()).build();
    }
}
