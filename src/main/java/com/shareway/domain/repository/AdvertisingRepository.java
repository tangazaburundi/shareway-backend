package com.shareway.domain.repository;

import com.shareway.domain.model.Advertising;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AdvertisingRepository extends JpaRepository<Advertising, String> {

    @Query("SELECT a FROM Advertising a WHERE a.active = true " +
           "AND (a.displayStart IS NULL OR a.displayStart <= :now) " +
           "AND (a.displayEnd IS NULL OR a.displayEnd >= :now) " +
           "ORDER BY a.sortOrder ASC, a.createdAt DESC")
    List<Advertising> findActiveAds(@Param("now") LocalDateTime now);

    @Query("SELECT a FROM Advertising a WHERE a.active = true " +
           "AND a.position = :position " +
           "AND (a.displayStart IS NULL OR a.displayStart <= :now) " +
           "AND (a.displayEnd IS NULL OR a.displayEnd >= :now) " +
           "ORDER BY a.sortOrder ASC, a.createdAt DESC")
    List<Advertising> findActiveByPosition(@Param("position") Advertising.AdvertisingPosition position,
                                           @Param("now") LocalDateTime now);

    Page<Advertising> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByActiveTrue();

    long countByPaymentStatus(Advertising.PaymentStatus status);
}
