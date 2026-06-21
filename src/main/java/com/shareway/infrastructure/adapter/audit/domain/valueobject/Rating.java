package com.shareway.infrastructure.adapter.audit.domain.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@EqualsAndHashCode
public final class Rating {
    private final BigDecimal value;

    public Rating(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.valueOf(5)) > 0)
            throw new IllegalArgumentException("Rating must be between 0 and 5");
        this.value = value.setScale(2, RoundingMode.HALF_UP);
    }

    public static Rating of(double value) {
        return new Rating(BigDecimal.valueOf(value));
    }

    public static Rating zero() {
        return new Rating(BigDecimal.ZERO);
    }

    public Rating recalculate(BigDecimal currentTotal, int currentCount, int newRating) {
        BigDecimal total = currentTotal.add(BigDecimal.valueOf(newRating));
        int count = currentCount + 1;
        return new Rating(total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP));
    }

    @Override
    public String toString() {
        return value.toPlainString();
    }
}
