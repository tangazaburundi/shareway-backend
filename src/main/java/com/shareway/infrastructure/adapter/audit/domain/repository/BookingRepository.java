package com.shareway.infrastructure.adapter.audit.domain.repository;

import com.shareway.infrastructure.adapter.audit.domain.model.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {

    Optional<Booking> findByTripIdAndPassengerId(String tripId, String passengerId);

    List<Booking> findByTripId(String tripId);

    /*    @Query("""
                    SELECT b
                    FROM Booking b
                    WHERE b.trip.driver.id = :driverId
                    AND b.deletedAt IS NULL
                """)
        List<Booking> findByDriverId(@Param("driverId") String driverId);*/
    //  List<Booking> findByTripDriverIdAndDeletedAtIsNull(String driverId);

    Optional<Booking> findByTripIdAndPassengerIdAndStatus(
            String tripId,
            String passengerId,
            Booking.BookingStatus status
    );

    boolean existsByTripIdAndPassengerIdAndDeletedAtIsNull(String tripId, String passengerId);

    List<Booking> findByPassengerIdAndDeletedAtIsNull(String passengerId);

    ///List<Booking> findByDriverIdAndDeletedAtIsNull(String driverId);

    @Query("SELECT b FROM Booking b WHERE b.passenger.id = :passengerId AND b.deletedAt IS NULL " +
            "ORDER BY b.createdAt DESC")
    Page<Booking> findByPassenger(@Param("passengerId") String passengerId, Pageable pageable);

    @Query("SELECT SUM(b.amountPaid) FROM Booking b WHERE b.passenger.id = :passengerId " +
            "AND b.status = 'COMPLETED' AND b.currency = :currency AND b.deletedAt IS NULL")
    BigDecimal sumSpentByPassenger(@Param("passengerId") String passengerId,
                                   @Param("currency") Booking.BookingStatus currency);

    @Query("SELECT SUM(b.amountPaid) FROM Booking b " +
            "WHERE b.trip.driver.id = :driverId AND b.status = 'CONFIRMED' " +
            "AND FUNCTION('MONTH', b.createdAt) = :month AND FUNCTION('YEAR', b.createdAt) = :year")
    BigDecimal sumEarningsByDriver(@Param("driverId") String driverId,
                                   @Param("month") int month, @Param("year") int year);

    @Query("SELECT COUNT(b) FROM Booking b WHERE FUNCTION('DATE', b.createdAt) = CURRENT_DATE AND b.deletedAt IS NULL")
    long countToday();
}
