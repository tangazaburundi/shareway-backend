package com.shareway.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Critères de recherche de trajets.
 * Seul departureCity est obligatoire — tous les autres filtres sont optionnels.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripSearchRequest {

    // ── Lieu (seul departureCity est requis) ─────────────────────────────
    private String departureCity;    // OBLIGATOIRE (au moins lui)
    private String arrivalCity;      // optionnel

    // ── Date / heure ─────────────────────────────────────────────────────
    private String date;             // "yyyy-MM-dd" — filtre sur le jour de départ
    private String departureTimeFrom;
    private String departureTimeTo;

    // ── Places & prix ────────────────────────────────────────────────────
    private Integer seats;
    private BigDecimal maxPrice;
    private String currency;

    // ── Conducteur ───────────────────────────────────────────────────────
    private Double minRating;

    // ── Préférences (tous optionnels) ─────────────────────────────────────
    private Boolean music;
    private Boolean smoking;
    private Boolean pets;
    private Boolean smallLuggage;   // filtrer trajets acceptant petite valise
    private Boolean largeLuggage;   // filtrer trajets acceptant grande valise
    private Boolean airConditioning;
}
