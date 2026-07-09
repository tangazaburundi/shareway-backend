package com.shareway.domain.repository;

import com.shareway.domain.model.Penalty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PenaltyRepository extends JpaRepository<Penalty, String> {
    List<Penalty> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Penalty> findByBookingId(String bookingId);
    long countByUserIdAndPaid(String userId, boolean paid);
}
