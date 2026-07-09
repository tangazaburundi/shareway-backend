package com.shareway.domain.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@EqualsAndHashCode
public final class GeoLocation {
    private final BigDecimal latitude;
    private final BigDecimal longitude;

    public GeoLocation(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                || latitude.compareTo(BigDecimal.valueOf(90)) > 0)
            throw new IllegalArgumentException("Invalid latitude: " + latitude);
        if (longitude == null || longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitude.compareTo(BigDecimal.valueOf(180)) > 0)
            throw new IllegalArgumentException("Invalid longitude: " + longitude);
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public static GeoLocation of(double lat, double lng) {
        return new GeoLocation(BigDecimal.valueOf(lat), BigDecimal.valueOf(lng));
    }

    /**
     * Calcul de distance Haversine en kilomètres
     */
    public double distanceTo(GeoLocation other) {
        final double R = 6371;
        double dLat = Math.toRadians(other.latitude.doubleValue() - this.latitude.doubleValue());
        double dLon = Math.toRadians(other.longitude.doubleValue() - this.longitude.doubleValue());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(this.latitude.doubleValue()))
                * Math.cos(Math.toRadians(other.latitude.doubleValue()))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    @Override
    public String toString() {
        return latitude.toPlainString() + "," + longitude.toPlainString();
    }
}
