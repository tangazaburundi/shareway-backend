package com.shareway.domain.repository;

import com.shareway.domain.model.Trip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, String>, JpaSpecificationExecutor<Trip> {

    Optional<Trip> findByIdAndDeletedAtIsNull(String id);

    Optional<Trip> findByShareTokenAndDeletedAtIsNull(String shareToken);

    @Query("SELECT t FROM Trip t WHERE t.driver.id = :driverId AND t.deletedAt IS NULL ORDER BY t.departureTime DESC")
    List<Trip> findByDriverId(@Param("driverId") String driverId);

    @Query("SELECT DISTINCT t FROM Trip t JOIN t.bookings b " +
            "WHERE b.passenger.id = :passengerId AND b.deletedAt IS NULL AND t.deletedAt IS NULL " +
            "ORDER BY t.departureTime DESC")
    List<Trip> findByPassengerId(@Param("passengerId") String passengerId);

    @Query("SELECT t FROM Trip t WHERE t.deletedAt IS NULL AND t.status = :status ORDER BY t.departureTime DESC")
    Page<Trip> findByStatus(@Param("status") Trip.TripStatus status, Pageable pageable);

    @Query("SELECT COUNT(t) FROM Trip t WHERE t.status = 'OPEN' AND t.deletedAt IS NULL")
    long countOpen();

    @Query("SELECT COUNT(t) FROM Trip t WHERE t.status = 'COMPLETED' AND t.deletedAt IS NULL")
    long countAllCompleted();

    @Query("SELECT COUNT(t) FROM Trip t WHERE FUNCTION('DATE', t.createdAt) = CURRENT_DATE AND t.deletedAt IS NULL")
    long countCreatedToday();

    /**
     * Utilisé par switchRole()
     */
    @Query("SELECT COUNT(t) FROM Trip t WHERE t.driver.id = :driverId AND t.status = 'OPEN' AND t.deletedAt IS NULL")
    long countOpenByDriver(@Param("driverId") String driverId);

    /**
     * Recherche flexible :
     * - departureCity est le seul filtre obligatoire (LIKE pour tolérance)
     * - tous les autres paramètres sont optionnels (null = ignoré)
     */
    @Query("""
            SELECT DISTINCT t FROM Trip t
            JOIN FETCH t.driver
            LEFT JOIN FETCH t.preferences pr
            WHERE t.deletedAt IS NULL
              AND t.status = 'OPEN'
              AND LOWER(t.departureCity) LIKE LOWER(CONCAT('%', :departureCity, '%'))
              AND (:arrivalCity   IS NULL OR LOWER(t.arrivalCity) LIKE LOWER(CONCAT('%', :arrivalCity, '%')))
              AND (:from          IS NULL OR t.departureTime >= :from)
              AND (:to            IS NULL OR t.departureTime <= :to)
              AND t.availableSeats >= :seats
              AND (:maxPrice      IS NULL OR t.pricePerSeat <= :maxPrice)
              AND (:minRating     IS NULL OR t.driver.rating >= :minRating)
              AND (:smallLuggage  IS NULL OR :smallLuggage = false OR (pr IS NOT NULL AND pr.smallLuggage = true))
              AND (:largeLuggage  IS NULL OR :largeLuggage = false OR (pr IS NOT NULL AND pr.largeLuggage = true))
              AND (:pets          IS NULL OR :pets          = false OR (pr IS NOT NULL AND pr.pets         = true))
            ORDER BY t.departureTime ASC
            """)
    Page<Trip> search(
            @Param("departureCity") String departureCity,
            @Param("arrivalCity") String arrivalCity,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("seats") int seats,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minRating") Double minRating,
            @Param("smallLuggage") Boolean smallLuggage,
            @Param("largeLuggage") Boolean largeLuggage,
            @Param("pets") Boolean pets,
            Pageable pageable
    );

    @Query("SELECT t FROM Trip t WHERE t.deletedAt IS NULL AND " +
            "t.departureTime BETWEEN :from AND :to AND t.status = 'OPEN'")
    List<Trip> findTripsInPeriod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
