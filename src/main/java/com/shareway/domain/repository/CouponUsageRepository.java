package com.shareway.domain.repository;

import com.shareway.domain.model.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, String> {
    long countByUserIdAndCouponId(String userId, String couponId);
}
