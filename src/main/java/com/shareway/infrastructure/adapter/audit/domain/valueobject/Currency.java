package com.shareway.infrastructure.adapter.audit.domain.valueobject;

import java.util.Map;

public enum Currency {
    FBU, USD, EUR;

    public static final Map<Currency, Map<Currency, Double>> RATES = Map.of(
            FBU, Map.of(FBU, 1.0, USD, 0.00034, EUR, 0.00031),
            USD, Map.of(FBU, 2900.0, USD, 1.0, EUR, 0.92),
            EUR, Map.of(FBU, 3200.0, USD, 1.09, EUR, 1.0)
    );

    public double convertTo(Currency target, double amount) {
        return amount * RATES.get(this).get(target);
    }

    public String symbol() {
        return switch (this) {
            case FBU -> "FBU";
            case USD -> "$";
            case EUR -> "€";
        };
    }
}
