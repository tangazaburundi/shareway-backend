package com.shareway.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Réponse du endpoint GET /users/me/stats
 * Correspond exactement à l'interface DashboardStats du frontend Angular.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    /** Nombre total de trajets créés (conducteur) ou réservés (passager) */
    private long totalTrips;

    /** Nombre de trajets effectués (COMPLETED) */
    private long completedTrips;

    /** Nombre total de passagers transportés (conducteur uniquement) */
    private long totalPassengers;

    /** Gains totaux dans la devise principale */
    private BigDecimal totalEarnings;

    /** Gains par devise : { "FBU": 50000, "USD": 20.0 } */
    private Map<String, BigDecimal> earningsByCurrency;

    /** Note moyenne (0.0 – 5.0) */
    private double rating;

    /** Nombre d'avis reçus */
    private int reviewCount;

    /** Taux de completion des trajets (%) */
    private double completionRate;

    /** Nombre de trajets à venir */
    private long upcomingTrips;

    /** Nombre de trajets annulés */
    private long cancelledTrips;

    /** Gains mensuels : [{ month: "2025-01", amount: 15000 }, ...] */
    private List<MonthlyEarning> monthlyEarnings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyEarning {
        /** Format "YYYY-MM" ex: "2025-06" */
        private String month;
        private BigDecimal amount;
    }
}
