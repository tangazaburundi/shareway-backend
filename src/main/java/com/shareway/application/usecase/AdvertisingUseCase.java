package com.shareway.application.usecase;

import com.shareway.application.dto.request.CreateAdvertisingRequest;
import com.shareway.application.dto.request.UpdateAdvertisingRequest;
import com.shareway.application.dto.response.AdvertisingResponse;
import com.shareway.application.port.out.AuditPort;
import com.shareway.application.port.out.StoragePort;
import com.shareway.domain.exception.AdvertisingNotFoundException;
import com.shareway.domain.exception.InvalidOperationException;
import com.shareway.domain.model.Advertising;
import com.shareway.domain.repository.AdvertisingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdvertisingUseCase {

    private final AdvertisingRepository advertisingRepository;
    private final AuditPort auditPort;
    private final StoragePort storagePort;

    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm[:ss]");

    public AdvertisingResponse create(CreateAdvertisingRequest req, String adminId) {
        Advertising ad = Advertising.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .imageUrl(req.getImageUrl())
                .linkUrl(req.getLinkUrl())
                .position(Advertising.AdvertisingPosition.valueOf(req.getPosition()))
                .active(false)
                .displayStart(req.getDisplayStart() != null ? LocalDateTime.parse(req.getDisplayStart(), DT_FORMAT) : null)
                .displayEnd(req.getDisplayEnd() != null ? LocalDateTime.parse(req.getDisplayEnd(), DT_FORMAT) : null)
                .sortOrder(req.getSortOrder())
                .targetAudience(req.getTargetAudience() != null ? req.getTargetAudience() : "ALL")
                .paymentStatus(req.getPaymentStatus() != null
                        ? Advertising.PaymentStatus.valueOf(req.getPaymentStatus())
                        : Advertising.PaymentStatus.FREE)
                .paymentAmount(req.getPaymentAmount())
                .paymentCurrency(req.getPaymentCurrency() != null ? req.getPaymentCurrency() : "FBU")
                .createdBy(adminId)
                .build();
        advertisingRepository.save(ad);
        auditPort.log("AD_CREATED", "Advertising", ad.getId(), null, req.getTitle(), adminId);
        log.info("Ad created: {} by admin {}", ad.getTitle(), adminId);
        return toResponse(ad);
    }

    @Transactional(readOnly = true)
    public AdvertisingResponse getById(String id) {
        Advertising ad = advertisingRepository.findById(id)
                .orElseThrow(() -> new AdvertisingNotFoundException("Publicité introuvable : " + id));
        return toResponse(ad);
    }

    @Transactional(readOnly = true)
    public Page<AdvertisingResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return advertisingRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toResponse);
    }

    public AdvertisingResponse update(String id, UpdateAdvertisingRequest req, String adminId) {
        Advertising ad = advertisingRepository.findById(id)
                .orElseThrow(() -> new AdvertisingNotFoundException("Publicité introuvable : " + id));

        if (req.getTitle() != null) ad.setTitle(req.getTitle());
        if (req.getDescription() != null) ad.setDescription(req.getDescription());
        if (req.getImageUrl() != null) ad.setImageUrl(req.getImageUrl());
        if (req.getLinkUrl() != null) ad.setLinkUrl(req.getLinkUrl());
        if (req.getPosition() != null) ad.setPosition(Advertising.AdvertisingPosition.valueOf(req.getPosition()));
        if (req.getDisplayStart() != null) ad.setDisplayStart(LocalDateTime.parse(req.getDisplayStart(), DT_FORMAT));
        if (req.getDisplayEnd() != null) ad.setDisplayEnd(LocalDateTime.parse(req.getDisplayEnd(), DT_FORMAT));
        if (req.getSortOrder() != null) ad.setSortOrder(req.getSortOrder());
        if (req.getTargetAudience() != null) ad.setTargetAudience(req.getTargetAudience());
        if (req.getPaymentStatus() != null) ad.setPaymentStatus(Advertising.PaymentStatus.valueOf(req.getPaymentStatus()));
        if (req.getPaymentAmount() != null) ad.setPaymentAmount(req.getPaymentAmount());
        if (req.getPaymentCurrency() != null) ad.setPaymentCurrency(req.getPaymentCurrency());

        advertisingRepository.save(ad);
        auditPort.log("AD_UPDATED", "Advertising", id, null, req.getTitle(), adminId);
        return toResponse(ad);
    }

    public void delete(String id, String adminId) {
        Advertising ad = advertisingRepository.findById(id)
                .orElseThrow(() -> new AdvertisingNotFoundException("Publicité introuvable : " + id));
        advertisingRepository.delete(ad);
        auditPort.log("AD_DELETED", "Advertising", id, ad.getTitle(), null, adminId);
    }

    public AdvertisingResponse activate(String id, String adminId) {
        Advertising ad = advertisingRepository.findById(id)
                .orElseThrow(() -> new AdvertisingNotFoundException("Publicité introuvable : " + id));
        ad.activate();
        advertisingRepository.save(ad);
        auditPort.log("AD_ACTIVATED", "Advertising", id, null, null, adminId);
        return toResponse(ad);
    }

    public AdvertisingResponse deactivate(String id, String adminId) {
        Advertising ad = advertisingRepository.findById(id)
                .orElseThrow(() -> new AdvertisingNotFoundException("Publicité introuvable : " + id));
        ad.deactivate();
        advertisingRepository.save(ad);
        auditPort.log("AD_DEACTIVATED", "Advertising", id, null, null, adminId);
        return toResponse(ad);
    }

    @Transactional(readOnly = true)
    public List<AdvertisingResponse> getActiveByPosition(String position) {
        Advertising.AdvertisingPosition pos = Advertising.AdvertisingPosition.valueOf(position);
        return advertisingRepository.findActiveByPosition(pos, LocalDateTime.now())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AdvertisingResponse> getAllActive() {
        return advertisingRepository.findActiveAds(LocalDateTime.now())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getStats() {
        java.util.Map<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("totalAds", advertisingRepository.count());
        stats.put("activeAds", advertisingRepository.countByActiveTrue());
        stats.put("paidAds", advertisingRepository.countByPaymentStatus(Advertising.PaymentStatus.PAID));
        stats.put("pendingPaymentAds", advertisingRepository.countByPaymentStatus(Advertising.PaymentStatus.PENDING));
        return stats;
    }

    public String uploadImage(MultipartFile file) {
        if (file.isEmpty()) throw new InvalidOperationException("Fichier vide");
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/"))
            throw new InvalidOperationException("Seuls les fichiers image sont acceptés");
        if (file.getSize() > 10 * 1024 * 1024)
            throw new InvalidOperationException("Le fichier ne doit pas dépasser 10 Mo");

        return storagePort.upload(file, "ads");
    }

    public void recordClick(String id) {
        advertisingRepository.findById(id).ifPresent(ad -> {
            ad.incrementClicks();
            advertisingRepository.save(ad);
        });
    }

    private AdvertisingResponse toResponse(Advertising ad) {
        return AdvertisingResponse.builder()
                .id(ad.getId())
                .title(ad.getTitle())
                .description(ad.getDescription())
                .imageUrl(ad.getImageUrl())
                .linkUrl(ad.getLinkUrl())
                .position(ad.getPosition().name())
                .active(ad.isActive())
                .displayStart(ad.getDisplayStart())
                .displayEnd(ad.getDisplayEnd())
                .sortOrder(ad.getSortOrder())
                .targetAudience(ad.getTargetAudience())
                .clicks(ad.getClicks())
                .impressions(ad.getImpressions())
                .paymentStatus(ad.getPaymentStatus().name())
                .paymentAmount(ad.getPaymentAmount())
                .paymentCurrency(ad.getPaymentCurrency())
                .createdBy(ad.getCreatedBy())
                .createdAt(ad.getCreatedAt())
                .updatedAt(ad.getUpdatedAt())
                .build();
    }
}
