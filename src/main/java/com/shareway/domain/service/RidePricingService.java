package com.shareway.domain.service;

import com.shareway.domain.model.PricingConfig;
import com.shareway.domain.model.Trip;
import com.shareway.domain.repository.PricingConfigRepository;
import com.shareway.domain.repository.RideRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RidePricingService {

    private final PricingConfigRepository pricingConfigRepository;
    private final RideRequestRepository rideRequestRepository;

    /**
     * Calcule le prix estimé d'une course.
     */
    public PricingResult calculatePrice(BigDecimal distanceKm, int durationMin, String currencyName) {
        Trip.Currency currency = parseCurrency(currencyName);
        PricingConfig config = pricingConfigRepository.findByCurrencyAndActiveTrue(currency)
                .orElseGet(() -> getDefaultConfig(currency));

        BigDecimal basePrice = config.getBasePrice();
        BigDecimal distancePrice = distanceKm.multiply(config.getPricePerKm());
        BigDecimal timePrice = BigDecimal.valueOf(durationMin).multiply(config.getPricePerMin());

        BigDecimal totalPrice = basePrice.add(distancePrice).add(timePrice);

        // Appliquer le minimum
        if (totalPrice.compareTo(config.getMinimumPrice()) < 0) {
            totalPrice = config.getMinimumPrice();
        }

        // Calculer le surge
        BigDecimal surgeMultiplier = calculateSurgeMultiplier(config);

        if (surgeMultiplier.compareTo(BigDecimal.ONE) > 0) {
            totalPrice = totalPrice.multiply(surgeMultiplier)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return PricingResult.builder()
                .estimatedPrice(totalPrice)
                .surgeMultiplier(surgeMultiplier)
                .basePrice(basePrice)
                .pricePerKm(config.getPricePerKm())
                .pricePerMin(config.getPricePerMin())
                .minimumPrice(config.getMinimumPrice())
                .platformFeePercent(config.getPlatformFeePercent())
                .currency(currency)
                .build();
    }

    /**
     * Calcule le multiplicateur de surge basé sur la demande actuelle.
     */
    private BigDecimal calculateSurgeMultiplier(PricingConfig config) {
        long activeSearches = rideRequestRepository.countActiveSearches();
        long activeRides = rideRequestRepository.countActiveRides();
        long totalDemand = activeSearches + activeRides;

        if (totalDemand <= config.getSurgeThreshold()) {
            return BigDecimal.ONE;
        }

        double excess = (double) (totalDemand - config.getSurgeThreshold());
        double multiplier = 1.0 + (excess * 0.1);
        multiplier = Math.min(multiplier, 3.0);

        return BigDecimal.valueOf(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    private PricingConfig getDefaultConfig(Trip.Currency currency) {
        return switch (currency) {
            case FBU -> PricingConfig.builder()
                    .currency(Trip.Currency.FBU)
                    .basePrice(new BigDecimal("1000"))
                    .pricePerKm(new BigDecimal("350"))
                    .pricePerMin(new BigDecimal("50"))
                    .minimumPrice(new BigDecimal("1500"))
                    .platformFeePercent(new BigDecimal("15.00"))
                    .build();
            case USD -> PricingConfig.builder()
                    .currency(Trip.Currency.USD)
                    .basePrice(new BigDecimal("1.00"))
                    .pricePerKm(new BigDecimal("0.35"))
                    .pricePerMin(new BigDecimal("0.05"))
                    .minimumPrice(new BigDecimal("1.50"))
                    .platformFeePercent(new BigDecimal("15.00"))
                    .build();
            case EUR -> PricingConfig.builder()
                    .currency(Trip.Currency.EUR)
                    .basePrice(new BigDecimal("0.90"))
                    .pricePerKm(new BigDecimal("0.30"))
                    .pricePerMin(new BigDecimal("0.04"))
                    .minimumPrice(new BigDecimal("1.35"))
                    .platformFeePercent(new BigDecimal("15.00"))
                    .build();
            case FRW -> PricingConfig.builder()
                    .currency(Trip.Currency.FRW)
                    .basePrice(new BigDecimal("500"))
                    .pricePerKm(new BigDecimal("175"))
                    .pricePerMin(new BigDecimal("25"))
                    .minimumPrice(new BigDecimal("750"))
                    .platformFeePercent(new BigDecimal("15.00"))
                    .build();
        };
    }

    private Trip.Currency parseCurrency(String currency) {
        if (currency == null || currency.isBlank()) return Trip.Currency.FBU;
        try {
            return Trip.Currency.valueOf(currency.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Trip.Currency.FBU;
        }
    }

    @lombok.Builder
    @lombok.Getter
    public static class PricingResult {
        private BigDecimal estimatedPrice;
        private BigDecimal surgeMultiplier;
        private BigDecimal basePrice;
        private BigDecimal pricePerKm;
        private BigDecimal pricePerMin;
        private BigDecimal minimumPrice;
        private BigDecimal platformFeePercent;
        private Trip.Currency currency;
    }
}
