package com.shareway.infrastructure.adapter.audit.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", nullable = false)
    private User targetUser;

    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewType type;

    @Column(name = "is_flagged")
    @Builder.Default
    private boolean flagged = false;

    @Column(name = "flag_reason")
    private String flagReason;

    @Column(name = "is_approved")
    @Builder.Default
    private boolean approved = true;

    @Column(name = "moderated_at")
    private LocalDateTime moderatedAt;

    @Column(name = "moderated_by")
    private String moderatedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public void flag(String reason) {
        this.flagged = true;
        this.flagReason = reason;
    }

    public void approve(String moderatorId) {
        this.approved = true;
        this.flagged = false;
        this.moderatedAt = LocalDateTime.now();
        this.moderatedBy = moderatorId;
    }

    public void reject(String moderatorId) {
        this.approved = false;
        this.moderatedAt = LocalDateTime.now();
        this.moderatedBy = moderatorId;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public enum ReviewType {DRIVER_TO_PASSENGER, PASSENGER_TO_DRIVER}
}
