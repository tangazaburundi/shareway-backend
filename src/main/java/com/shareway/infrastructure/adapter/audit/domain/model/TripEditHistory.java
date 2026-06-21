package com.shareway.infrastructure.adapter.audit.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Trace toutes les modifications apportées à un trajet.
 * Un enregistrement par champ modifié (granularité fine pour l'audit).
 */
@Entity
@Table(name = "trip_edit_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripEditHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "trip_id", nullable = false)
    private String tripId;

    @Column(name = "edited_by", nullable = false)
    private String editedBy;

    /**
     * Nom du champ modifié, ex: "departureCity", "pricePerSeat", "departureTime"
     */
    @Column(name = "field_changed", nullable = false)
    private String fieldChanged;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "notification_sent")
    @Builder.Default
    private boolean notificationSent = false;

    @Column(name = "passengers_notified")
    @Builder.Default
    private int passengersNotified = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}