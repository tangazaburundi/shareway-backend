package com.shareway.application.usecase;

import com.shareway.application.dto.response.PartenaireResponse;
import com.shareway.application.port.out.AuditPort;
import com.shareway.application.port.out.StoragePort;
import com.shareway.domain.exception.InvalidOperationException;
import com.shareway.domain.exception.PartenaireNotFoundException;
import com.shareway.domain.model.Partenaire;
import com.shareway.domain.repository.PartenaireRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PartenaireUseCase {

    private final PartenaireRepository partenaireRepository;
    private final AuditPort auditPort;
    private final StoragePort storagePort;

    @Transactional(readOnly = true)
    public List<PartenaireResponse> getAllActive() {
        return partenaireRepository.findByActifTrueOrderBySortOrderAsc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PartenaireResponse> getAllForAdmin() {
        return partenaireRepository.findAllByOrderBySortOrderAsc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PartenaireResponse getById(String id) {
        Partenaire p = partenaireRepository.findById(id)
                .orElseThrow(() -> new PartenaireNotFoundException("Partenaire introuvable : " + id));
        return toResponse(p);
    }

    public PartenaireResponse create(String nom, String imageUrl, String lienUrl, Integer sortOrder, String adminId) {
        Partenaire p = Partenaire.create(nom, imageUrl, lienUrl, sortOrder != null ? sortOrder : 0);
        partenaireRepository.save(p);
        auditPort.log("PARTENAIRE_CREATED", "Partenaire", p.getId(), null, nom, adminId);
        log.info("Partenaire created: {} by admin {}", nom, adminId);
        return toResponse(p);
    }

    public PartenaireResponse update(String id, String nom, String imageUrl, String lienUrl,
                                     Boolean actif, Integer sortOrder, String adminId) {
        Partenaire p = partenaireRepository.findById(id)
                .orElseThrow(() -> new PartenaireNotFoundException("Partenaire introuvable : " + id));
        p.update(nom, imageUrl, lienUrl, actif, sortOrder);
        partenaireRepository.save(p);
        auditPort.log("PARTENAIRE_UPDATED", "Partenaire", id, null, nom, adminId);
        return toResponse(p);
    }

    public PartenaireResponse toggleActive(String id, String adminId) {
        Partenaire p = partenaireRepository.findById(id)
                .orElseThrow(() -> new PartenaireNotFoundException("Partenaire introuvable : " + id));
        p.setActif(!p.isActif());
        partenaireRepository.save(p);
        auditPort.log("PARTENAIRE_TOGGLED", "Partenaire", id, null, p.isActif(), adminId);
        log.info("Partenaire {} {} by admin {}", p.getNom(), p.isActif() ? "activated" : "deactivated", adminId);
        return toResponse(p);
    }

    public void delete(String id, String adminId) {
        Partenaire p = partenaireRepository.findById(id)
                .orElseThrow(() -> new PartenaireNotFoundException("Partenaire introuvable : " + id));
        partenaireRepository.delete(p);
        auditPort.log("PARTENAIRE_DELETED", "Partenaire", id, p.getNom(), null, adminId);
        log.info("Partenaire deleted: {} by admin {}", p.getNom(), adminId);
    }

    public String uploadImage(MultipartFile file) {
        if (file.isEmpty()) throw new InvalidOperationException("Fichier vide");
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/"))
            throw new InvalidOperationException("Seuls les fichiers image sont acceptés");
        if (file.getSize() > 10 * 1024 * 1024)
            throw new InvalidOperationException("Le fichier ne doit pas dépasser 10 Mo");

        return storagePort.upload(file, "partenaires");
    }

    private PartenaireResponse toResponse(Partenaire p) {
        return PartenaireResponse.builder()
                .id(p.getId())
                .nom(p.getNom())
                .imageUrl(p.getImageUrl())
                .lienUrl(p.getLienUrl())
                .actif(p.isActif())
                .sortOrder(p.getSortOrder())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
