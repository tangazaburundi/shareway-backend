package com.shareway.domain.repository;

import com.shareway.domain.model.DriverAvailability;
import com.shareway.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverAvailabilityRepository extends JpaRepository<DriverAvailability, String> {

    Optional<DriverAvailability> findByUser(User user);

    @Query("SELECT d FROM DriverAvailability d WHERE d.isAvailable = true AND d.status = 'AVAILABLE' " +
            "AND d.currentLat IS NOT NULL AND d.currentLng IS NOT NULL")
    List<DriverAvailability> findAllAvailable();

    @Query("SELECT d FROM DriverAvailability d WHERE d.user.id = :driverId")
    Optional<DriverAvailability> findByDriverId(@Param("driverId") String driverId);

    @Query("SELECT COUNT(d) FROM DriverAvailability d WHERE d.status = 'AVAILABLE' AND d.isAvailable = true")
    long countAvailableDrivers();

    @Query("SELECT d FROM DriverAvailability d WHERE d.isAvailable = true " +
            "AND d.status = 'AVAILABLE' " +
            "AND d.currentLat IS NOT NULL AND d.currentLng IS NOT NULL " +
            "AND (6371 * ACOS(COS(RADIANS(:lat)) * COS(RADIANS(d.currentLat)) * " +
            "COS(RADIANS(d.currentLng) - RADIANS(:lng)) + " +
            "SIN(RADIANS(:lat)) * SIN(RADIANS(d.currentLat)))) <= :radiusKm " +
            "ORDER BY (6371 * ACOS(COS(RADIANS(:lat)) * COS(RADIANS(d.currentLat)) * " +
            "COS(RADIANS(d.currentLng) - RADIANS(:lng)) + " +
            "SIN(RADIANS(:lat)) * SIN(RADIANS(d.currentLat)))) ASC")
    List<DriverAvailability> findNearbyDriversWithinRadius(@Param("lat") double lat, @Param("lng") double lng, @Param("radiusKm") int radiusKm);
}
