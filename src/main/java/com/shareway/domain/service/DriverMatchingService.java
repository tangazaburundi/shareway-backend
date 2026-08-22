package com.shareway.domain.service;

import com.shareway.domain.model.DriverAvailability;
import com.shareway.domain.model.Vehicle;
import com.shareway.domain.repository.DriverAvailabilityRepository;
import com.shareway.domain.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverMatchingService {

    private final DriverAvailabilityRepository driverAvailabilityRepository;
    private final VehicleRepository vehicleRepository;

    public static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Trouve les chauffeurs proches d'une position donnée, triés par distance.
     */
    public List<DriverAvailability> findNearbyDrivers(double lat, double lng, int radiusKm) {
        return driverAvailabilityRepository.findNearbyDriversWithinRadius(lat, lng, radiusKm);
    }

    /**
     * Calcule la distance entre deux points en km (Haversine).
     */
    public double calculateDistanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Calcule la distance entre deux points et retourne en BigDecimal.
     */
    public BigDecimal calculateDistance(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
        double dist = calculateDistanceKm(
                lat1.doubleValue(), lng1.doubleValue(),
                lat2.doubleValue(), lng2.doubleValue());
        return BigDecimal.valueOf(dist).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Estime la durée en minutes (basé sur vitesse moyenne 30 km/h en ville).
     */
    public int estimateDurationMinutes(BigDecimal distanceKm) {
        if (distanceKm == null) return 0;
        double hours = distanceKm.doubleValue() / 30.0;
        return Math.max(1, (int) Math.round(hours * 60));
    }

    /**
     * Récupère le véhicule principal d'un chauffeur.
     */
    public Vehicle getDriverVehicle(String driverId) {
        return vehicleRepository.findByUserId(driverId).stream()
                .filter(Vehicle::isActive)
                .findFirst()
                .orElse(null);
    }
}
