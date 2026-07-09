package com.shareway.domain.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PriceCalculationService {

    private static final BigDecimal FUEL_COST_PER_KM = new BigDecimal("0.10");
    private static final BigDecimal PLATFORM_COMMISSION = new BigDecimal("0.15");
    private static final BigDecimal MIN_PRICE = new BigDecimal("2.00");

    public double estimateDistance(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public BigDecimal suggestPricePerSeat(double distanceKm, int totalSeats) {
        if (distanceKm <= 0) return MIN_PRICE;
        BigDecimal fuelCost = BigDecimal.valueOf(distanceKm).multiply(FUEL_COST_PER_KM);
        BigDecimal costPerSeat = fuelCost.divide(BigDecimal.valueOf(totalSeats), 2, RoundingMode.HALF_UP);
        BigDecimal withCommission = costPerSeat.divide(BigDecimal.ONE.subtract(PLATFORM_COMMISSION), 2, RoundingMode.HALF_UP);
        return withCommission.compareTo(MIN_PRICE) > 0 ? withCommission : MIN_PRICE;
    }
}
