package com.shareway.infrastructure.adapter.audit.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "trip_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripPreferences {

    @Id
    private String tripId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @Builder.Default
    private boolean music = false;
    @Builder.Default
    private boolean smoking = false;
    @Builder.Default
    private boolean pets = false;  // animaux autorisés
    @Builder.Default
    private boolean talking = false;

    @Column(name = "air_conditioning")
    @Builder.Default
    private boolean airConditioning = false;

    // ── Nouvelles options bagages ──────────────────────────────────────
    @Column(name = "small_luggage")
    @Builder.Default
    private boolean smallLuggage = true;  // petite valise ok

    @Column(name = "large_luggage")
    @Builder.Default
    private boolean largeLuggage = false;  // grande valise ok
}
