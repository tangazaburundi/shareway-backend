package com.shareway.domain.repository;

import com.shareway.domain.model.RideRequest;
import com.shareway.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RideRequestRepository extends JpaRepository<RideRequest, String> {

    Optional<RideRequest> findById(String id);

    @Query("SELECT r FROM RideRequest r WHERE r.passenger.id = :passengerId " +
            "AND r.status IN ('SEARCHING', 'DRIVER_FOUND', 'ACCEPTED', 'DRIVER_EN_ROUTE', 'ARRIVED', 'IN_PROGRESS') " +
            "ORDER BY r.createdAt DESC")
    Optional<RideRequest> findActiveByPassengerId(@Param("passengerId") String passengerId);

    @Query("SELECT r FROM RideRequest r WHERE r.driver.id = :driverId " +
            "AND r.status IN ('DRIVER_FOUND', 'ACCEPTED', 'DRIVER_EN_ROUTE', 'ARRIVED', 'IN_PROGRESS') " +
            "ORDER BY r.createdAt DESC")
    Optional<RideRequest> findActiveByDriverId(@Param("driverId") String driverId);

    @Query("SELECT r FROM RideRequest r WHERE r.status = 'SEARCHING' AND r.driver IS NULL " +
            "AND r.searchStartedAt < :timeout ORDER BY r.searchStartedAt ASC")
    List<RideRequest> findExpiredSearches(@Param("timeout") LocalDateTime timeout);

    @Query("SELECT r FROM RideRequest r WHERE r.status = 'SEARCHING' AND r.driver IS NULL " +
            "AND r.searchStartedAt >= :since ORDER BY r.searchStartedAt ASC")
    List<RideRequest> findRecentSearches(@Param("since") LocalDateTime since);

    @Query("SELECT r FROM RideRequest r WHERE r.driver.id = :driverId ORDER BY r.createdAt DESC")
    List<RideRequest> findByDriverIdOrderByCreatedAtDesc(@Param("driverId") String driverId);

    @Query("SELECT r FROM RideRequest r WHERE r.passenger.id = :passengerId ORDER BY r.createdAt DESC")
    List<RideRequest> findByPassengerIdOrderByCreatedAtDesc(@Param("passengerId") String passengerId);

    @Query("SELECT COUNT(r) FROM RideRequest r WHERE r.status = 'SEARCHING' AND r.driver IS NULL")
    long countActiveSearches();

    @Query("SELECT COUNT(r) FROM RideRequest r WHERE r.status IN ('ACCEPTED', 'DRIVER_EN_ROUTE', 'IN_PROGRESS')")
    long countActiveRides();

    @Query("SELECT r FROM RideRequest r WHERE r.driver.id = :driverId AND r.status IN ('SEARCHING', 'DRIVER_FOUND') ORDER BY r.searchStartedAt ASC")
    List<RideRequest> findPendingOffersForDriver(@Param("driverId") String driverId);

    List<RideRequest> findByDriverAndStatus(User driver, RideRequest.RideStatus status);

    @Query("SELECT r FROM RideRequest r WHERE r.status = 'DRIVER_FOUND' " +
            "AND r.driverNotifiedAt IS NOT NULL AND r.driverNotifiedAt < :cutoff")
    List<RideRequest> findExpiredDriverFound(@Param("cutoff") LocalDateTime cutoff);
}
