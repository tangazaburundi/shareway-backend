package com.shareway.domain.repository;

import com.shareway.domain.model.PaymentRefusal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRefusalRepository extends JpaRepository<PaymentRefusal, String> {
    List<PaymentRefusal> findByUserIdOrderByCreatedAtDesc(String userId);
    List<PaymentRefusal> findByResolvedFalseOrderByCreatedAtDesc();
    Optional<PaymentRefusal> findByRideIdAndResolvedFalse(String rideId);
    List<PaymentRefusal> findAllByOrderByCreatedAtDesc();
}
