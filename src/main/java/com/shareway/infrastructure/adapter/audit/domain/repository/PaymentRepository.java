package com.shareway.infrastructure.adapter.audit.domain.repository;

import com.shareway.infrastructure.adapter.audit.domain.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {
    List<Payment> findByUserIdOrderByCreatedAtDesc(String userId);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.user.id = :userId AND p.status = 'SUCCEEDED' AND p.currency = :currency")
    BigDecimal sumByUserAndCurrency(@Param("userId") String userId, @Param("currency") Payment.PaymentCurrency currency);
}
